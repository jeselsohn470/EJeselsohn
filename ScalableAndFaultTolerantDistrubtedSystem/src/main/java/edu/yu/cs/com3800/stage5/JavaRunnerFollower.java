package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class JavaRunnerFollower extends Thread implements LoggingServer {
    private ZooKeeperPeerServerImpl zooKeeperPeerServer;
    private InetSocketAddress myAddress;
    private  Map<Long, InetSocketAddress> peerIDtoAddress;
    private LinkedBlockingQueue<Message> incomingMessages;
    private List<Integer> observerPorts;
    private JavaRunner javaRunner;
    private volatile boolean shutdown = false;
    private boolean errorHappened = false;
    private GossipThread gossipThread;
    private ServerSocket serverSocket = null;
    private Message completedWork;
    private Map<Integer, Long> portToId = new HashMap<>();

    private ConcurrentLinkedDeque<String> messages = new ConcurrentLinkedDeque<>();
    private Logger logger;
    private int gossipServerPort;

    public JavaRunnerFollower(ZooKeeperPeerServerImpl peerServer, InetSocketAddress InetSocketAddress, Map<Long, InetSocketAddress> peerIDtoAddress, LinkedBlockingQueue<Message> messages, List<Integer> observerPorts, int gossipServerPort){
        this.myAddress = InetSocketAddress;
        this.zooKeeperPeerServer= peerServer;
        this.gossipServerPort = gossipServerPort;
        this.peerIDtoAddress = peerIDtoAddress;
        this.incomingMessages = messages;
        this.observerPorts = observerPorts;
        for (Long id: peerIDtoAddress.keySet()){
            this.portToId.put(peerIDtoAddress.get(id).getPort(), id);
        }
        this.setDaemon(true);
        this.gossipThread = new GossipThread(this.zooKeeperPeerServer, this.incomingMessages, this. peerIDtoAddress, this.observerPorts, gossipServerPort);
        this.gossipThread.start();
        this.setDaemon(true);
        try {
            this.javaRunner = new JavaRunner();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.logger = initializeLogging(JavaRunnerFollower.class.getCanonicalName() + " on-port: " + (this.myAddress.getPort() + 2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void shutdownGossipThread(){
        this.gossipThread.shutdown();
//        this.gossipThread.interrupt();
    }

    public void shutdown(){
        this.logger.fine("JRF shutdown on port " + this.zooKeeperPeerServer.getUdpPort());
        interrupt();
        this.shutdown = true;
        this.gossipThread.shutdown();

        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Message returnWork(){
        return this.completedWork;
    }

    @Override
    public void run() {
        try{
            this.logger.fine("Java Runner Follower Started");
            serverSocket = new ServerSocket(this.myAddress.getPort() + 2);
            boolean noLeader = false;
            while (!this.shutdown){
                if (this.zooKeeperPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.LOOKING || this.zooKeeperPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.LEADING){
                    continue;
                }
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                }catch(SocketException e){

                    break;
                }
                InputStream input = clientSocket.getInputStream();
                logger.fine("Got message from leader");
                byte[] receivedBytes = Util.readAllBytesFromNetwork(input);
                Message msg = new Message(receivedBytes);

                OutputStream os = clientSocket.getOutputStream();
                logger.fine("Message to compile:");
                logger.fine(msg.toString());
                byte[] messageBytes = msg.getMessageContents();
                String returnedCode = null;
                //queue the results of any client work they have completed, until a new leader is elected
                if(msg.getMessageType() == Message.MessageType.NEW_LEADER_GETTING_LAST_WORK){
                    //Message type is one trying to retrieve information
                    if (this.completedWork == null){
                        Message message = new Message(Message.MessageType.COMPLETED_WORK, new byte[0], "", 0, "", 0, -1, true);
                        os.write(message.getNetworkPayload());
                        os.close();
                        continue;
                    }
                   os.write(completedWork.getNetworkPayload());
                   completedWork = null;
                   os.close();
                   continue;
                } else {
                    //If message type isn't one trying to retrieve information
                    InputStream inputstreamtoRRL = new ByteArrayInputStream(messageBytes);

                     returnedCode = readMessage(inputstreamtoRRL);
                }
                Message sendMessage = getMessage(returnedCode, msg.getRequestID());
//                completedWork = sendMessage;
                logger.fine("Message being sent back");
                logger.fine(sendMessage.toString());

                //queue the results of any client work they have completed, until a new leader is elected
                if (this.gossipThread.checkLeaderFailed(this.peerIDtoAddress.get(this.zooKeeperPeerServer.getCurrentLeader().getProposedLeaderID()).getPort())){
                    this.completedWork = sendMessage;
                    continue;
                }
//                if (sendMessage.getRequestID() == 4){
//                    try {
//                        sleep(100000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                os.write(sendMessage.getNetworkPayload());
                logger.fine("Message sent back to leader");
                os.close();
//
//                this.logger.info("The following message was sent back to Round Robin Leader: " + messageBackToRRL.toString());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    //reading the incoming message and return the code if no error occurred or the right message if an error occurred
    private String readMessage(InputStream message){
        String code = null;
        try{
            code = this.javaRunner.compileAndRun(message);
            setDidErrorHappened(false); //no error happened
        } catch (Exception e){
            setDidErrorHappened(true); //an error occurred
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            String output = null;
            try{
                output = baos.toString("UTF8");
            } catch (UnsupportedEncodingException ex){
                ex.printStackTrace();
            }
            code = e.getMessage() + '\n' + output;
        }
        return code;
    }

    //create the message that will be sent back to the Round Robin Leader
    private Message getMessage(String code, long requestID){
        int port = this.zooKeeperPeerServer.getPeerByID(this.zooKeeperPeerServer.getCurrentLeader().getProposedLeaderID()).getPort() + 2;
        String hostname = this.zooKeeperPeerServer.getPeerByID(this.zooKeeperPeerServer.getCurrentLeader().getProposedLeaderID()).getHostName();
        return new Message(Message.MessageType.COMPLETED_WORK, code.getBytes(), this.myAddress.getHostString(), this.myAddress.getPort()+2, hostname, port, requestID, this.didErrorHappen());
    }
    protected boolean didErrorHappen(){
        return this.errorHappened;
    }
    protected void setDidErrorHappened(boolean errorHappened){
        this.errorHappened = errorHappened;
    }


}

