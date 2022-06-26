package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


public class ZooKeeperPeerServerImpl extends Thread implements ZooKeeperPeerServer, LoggingServer{
    private final InetSocketAddress myAddress;
    private final int myPort;
    private volatile ServerState state = ServerState.LOOKING;
    private volatile boolean shutdown = false;
    private LinkedBlockingQueue<Message> incomingMessages;
    private LinkedBlockingQueue<Message> outgoingMessages;

    private LinkedBlockingQueue<Message> incomingGossipMessages;
    private Long id;
    private long peerEpoch;
    private volatile Vote currentLeader;
    private Map<Long,InetSocketAddress> peerIDtoAddress;
    private UDPMessageSender sender;
    private UDPMessageReceiver receiver;
    private List<Integer> observerPorts;
    private boolean leaderAccepted = false;
    private JavaRunnerFollower javaRunnerFollower = null;
    private RoundRobinLeader roundRobinLeader = null;
    private Logger logger;

    private GossipThread gossipThread;

    private boolean newLeader = false;
    private Set<Long> failedNodes = new HashSet<>();
    private Set<InetSocketAddress> failedInet = new HashSet<>();
    private ConcurrentHashMap<byte[], String> workMap = new ConcurrentHashMap<>();
    private boolean leaderDied = false;


    public ZooKeeperPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long,InetSocketAddress> peerIDtoAddress, List<Integer> observerPorts){
        this.incomingMessages = new LinkedBlockingQueue<>();
        this.outgoingMessages = new LinkedBlockingQueue<>();
        this.incomingGossipMessages = new LinkedBlockingQueue<>();
        this.myPort = myPort;
        this.peerEpoch  = peerEpoch;
        this.id = id;
        this.myAddress = new InetSocketAddress("localhost", this.myPort);
        this.observerPorts = observerPorts;
        this.peerIDtoAddress = peerIDtoAddress;
        this.peerIDtoAddress.remove(id);
        this.currentLeader = new Vote(this.id, this.peerEpoch);
        try {
            this.logger = initializeLogging(ZooKeeperPeerServerImpl.class.getCanonicalName()+ " on-port: " + (this.myAddress.getPort() + 2));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try{
            //step 1: create and run thread that sends broadcast messages
            this.sender = new UDPMessageSender(this.outgoingMessages, this.myPort);
            this.sender.start();
            this.logger.fine("sender started");
            //step 2: create and run thread that listens for messages sent to this server
            //changed the passed in server to this, since I was getting a null in the UDP message receiver
            this.receiver = new UDPMessageReceiver(this.incomingGossipMessages, this.incomingMessages, this.myAddress, this.myPort,this);
            this.receiver.start();
            this.logger.fine("receiver started");
        }catch(IOException e){
            e.printStackTrace();
        }


    }

    @Override
    public void shutdown(){
        //shut down the receiver and sender
        this.shutdown = true;
        this.logger.fine("shutdown the receiver and sender");
        this.receiver.shutdown();
        this.sender.shutdown();
//        if (this.getPeerState() == ServerState.OBSERVER && !this.gossipThread.isDead()){
//            this.gossipThread.shutdown();
//            this.gossipThread.interrupt();
//        }
        if (this.gossipThread != null){
            this.gossipThread.shutdown();
//            this.gossipThread.interrupt();
        }
        try {
            sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setCurrentLeader(Vote v) throws IOException {
        this.peerEpoch = this.currentLeader.getPeerEpoch();
        this.currentLeader = v;
    }

    @Override
    public Vote getCurrentLeader() {
        return this.currentLeader;

    }

//    public void sendGossipMessage(Message.MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException {
//        Message msg = new Message(type, messageContents, this.myAddress.getHostString(), this.myPort, target.getHostString(), target.getPort());
//        this.outgoingGossipMessages.offer(msg);
//    }

    @Override
    public void sendMessage(Message.MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException {
        Message msg = new Message(type, messageContents, this.myAddress.getHostString(), this.myPort, target.getHostString(), target.getPort());
        this.outgoingMessages.offer(msg);
    }

    @Override
    public void sendBroadcast(Message.MessageType type, byte[] messageContents) {
        for(InetSocketAddress peer : this.peerIDtoAddress.values()){
            Message msg = new Message(type, messageContents, this.myAddress.getHostString(), this.myPort,peer.getHostString(), peer.getPort());
            this.outgoingMessages.offer(msg);
        }
    }

    @Override
    public ServerState getPeerState() {
        return this.state;
    }

    @Override
    public void setPeerState(ServerState newState) {
        this.state = newState;
    }

    @Override
    public Long getServerId() {
        return this.id;
    }

    @Override
    public long getPeerEpoch() {
        return this.peerEpoch;
    }

    @Override
    public InetSocketAddress getAddress() {
        return this.myAddress;
    }

    @Override
    public int getUdpPort() {
        return this.myPort;
    }

    @Override
    public InetSocketAddress getPeerByID(long peerId) {
        if (this.peerIDtoAddress.containsKey(peerId)){
            return this.peerIDtoAddress.get(peerId);
        }
        return null;

    }

    @Override
    public int getQuorumSize() {
        //gets rid of the observerPorts from the quorom size
        if (this.peerIDtoAddress.size() % 2 ==0){
            return ((this.peerIDtoAddress.size() + 1 - this.observerPorts.size()) / 2);
        }
        else{
            return ((this.peerIDtoAddress.size() + 1 - this.observerPorts.size()) / 2) + 1;
        }
    }


//    public boolean newLeader(){
//        return this.newLeader;
//    }
//    public void setNewLeader(boolean newLeader){
//        this.newLeader = newLeader;
//    }


    public GossipThread getGossipThread(){
        return this.gossipThread;
    }

    public boolean didLeaderDie(){
        return  this.leaderDied;
    }

    public void removeLeader(){

        this.leaderDied = true;
        this.peerEpoch++;
        this.peerIDtoAddress.remove(this.currentLeader.getProposedLeaderID());
        Vote newVote = new Vote(this.id, this.peerEpoch);
        try {
            this.setCurrentLeader(newVote);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.logger.fine("I with the ID of " + this.getServerId() + " have this as my leader: "+ this.currentLeader.getProposedLeaderID());
        this.setPeerState(ServerState.LOOKING);

    }

    public void serverFailed(long id){
        if(this.getPeerState() == ZooKeeperPeerServer.ServerState.LEADING){
            this.roundRobinLeader.failedServer(id);
        }
        this.peerIDtoAddress.remove(id);

    }


    public void setLeaderAccepted(boolean leaderAccepted){
        this.peerEpoch++;
        this.peerIDtoAddress.remove(this.currentLeader.getProposedLeaderID());
        Vote newVote = new Vote(this.id, this.peerEpoch);
        try {
            this.setCurrentLeader(newVote);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.leaderAccepted = leaderAccepted;
        this.logger.fine("I with the ID of " + this.getServerId() + " have this as my leader: "+ this.currentLeader.getProposedLeaderID());
        this.leaderDied = true;

    }

    public void addToFailed(Long id){

        this.failedNodes.add(id);
        this.failedInet.add(this.peerIDtoAddress.get(id));
    }



    @Override
    public boolean isPeerDead(long peerID) {
        return this.failedNodes.contains(peerID);
    }

    @Override
    public boolean isPeerDead(InetSocketAddress address){
        return this.failedInet.contains(address);
    }





    @Override
    public void run(){
        try{
            int increase = 0;
            while (!this.shutdown){
                switch (getPeerState()){
                    case OBSERVER:
                        if(!this.leaderAccepted){
                            this.leaderAccepted = true;
                            this.logger.fine("Leader Election is Triggered by the observer.");
                            if (this.gossipThread != null){
                                this.gossipThread.shutdown();
                            }
                            ZooKeeperLeaderElection zooKeeperLeaderElection = new ZooKeeperLeaderElection(this, this.incomingMessages);
                            Vote vote = zooKeeperLeaderElection.lookForLeader();
                            setCurrentLeader(vote);
                            this.gossipThread = new GossipThread(this, this.incomingGossipMessages, this.peerIDtoAddress, this.observerPorts, this.myPort + increase);
                            increase++;
                            this.gossipThread.start();


                        }
                        break;
                    case LOOKING:
                        this.logger.fine("Leader Election is Triggered by: " + this.getServerId());
                        ZooKeeperLeaderElection zooKeeperLeaderElection = new ZooKeeperLeaderElection(this, this.incomingMessages);
                        Vote vote = zooKeeperLeaderElection.lookForLeader();
                        setCurrentLeader(vote);
                        break;
                    case LEADING:
                        if(this.roundRobinLeader == null ){
                            this.logger.fine("Round Robin Leader started.");
                            this.outgoingMessages.clear();
                            Message oldWork = null;
                            if (this.javaRunnerFollower != null){
                                oldWork = this.javaRunnerFollower.returnWork();
                                this.javaRunnerFollower.shutdown();
                                this.javaRunnerFollower = null;

                            }
                            this.roundRobinLeader = new RoundRobinLeader(this, this.myAddress, this.peerIDtoAddress, this.incomingGossipMessages,this.observerPorts, oldWork, this.myPort + increase);
                            increase++;
                            this.roundRobinLeader.start();
                        }
//                        if (this.javaRunnerFollower != null){
//                            this.javaRunnerFollower.shutdown();
//                            this.javaRunnerFollower = null;
//                        }
                        break;
                    case FOLLOWING:
                        if (this.javaRunnerFollower == null){
                            this.logger.fine("Java Runner Follower started.");
                            this.outgoingMessages.clear();
                            this.javaRunnerFollower = new JavaRunnerFollower(this, this.myAddress, this.peerIDtoAddress, this.incomingGossipMessages, this.observerPorts, this.myPort + increase);
                            increase++;
                            this.javaRunnerFollower.start();
                        }
                        break;

                }
            }
            //shutting down the JRF
            if(getPeerState() == ServerState.FOLLOWING){
                this.logger.fine("JRF shutdown.");
                this.javaRunnerFollower.shutdown();
                this.javaRunnerFollower.shutdownGossipThread();
            }
            //shutting down the RRL
            else if(getPeerState() == ServerState.LEADING){
                this.logger.fine("RRL shutdown.");
                this.roundRobinLeader.shutdown();
            }


        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }


}
