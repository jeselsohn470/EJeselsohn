package edu.yu.cs.com3800.stage5;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class GossipThread extends Thread implements LoggingServer {

    private ZooKeeperPeerServerImpl zooKeeperPeerServer;
    private LinkedBlockingQueue<Message> incomingMessages;
    private Map<Long, InetSocketAddress> peerIds;
    private Map<Long, Node> nodeMap = new HashMap<>();
    private Queue<Integer> ports = new LinkedList<>();
    private Set<Long> failedServers = ConcurrentHashMap.newKeySet();
    private Map<Integer, Long> portToId = new HashMap<>();
    private List<Integer> observerPorts;
    private long start;
    private long current;
    private boolean dead = false;
    private List<Gossip> gossip = new ArrayList<>();

//    private ThreadPoolExecutor threadPoolExecutor;
    private HttpServer httpServer;
    private Logger gossipLogger;
    private int gossipServerPort;
    private ThreadPoolExecutor threadPoolExecutor;



    private boolean shutdown = false;
    private static final int GOSSIP = 3000;
    private static final int FAIL = GOSSIP * 13;
    private  static final int CLEANUP = FAIL * 2;
    private  static final int finalizeWait = 200;
    private static final int maxNotificationInterval = 60000;
    private long myOwnHeartbeat = 0;
    private Logger logger;
    private Map<Long, InetSocketAddress> peerIdsCopy = new HashMap<>();
    private Set<Long> cleanupedServers = new HashSet<>();
    private ConcurrentHashMap<Long, Node> cleanupedServersMap = new ConcurrentHashMap<>();


    public GossipThread(ZooKeeperPeerServerImpl zooKeeperPeerServer, LinkedBlockingQueue<Message> incomingMessages, Map<Long, InetSocketAddress> peerIDs, List<Integer> observerPorts, int gossipServerPort){
        this.zooKeeperPeerServer = zooKeeperPeerServer;
        this.incomingMessages = incomingMessages;
        this.peerIds = peerIDs;
        for (Long id : peerIDs.keySet()){
            peerIdsCopy.put(id, peerIDs.get(id));
        }
        this.observerPorts = observerPorts;
        for (InetSocketAddress port: this.peerIds.values()){
            this.ports.add(port.getPort());
        }
        this.ports.addAll(this.observerPorts);
        Collections.shuffle((List<?>) this.ports);

        for (Long id: peerIDs.keySet()){
            this.portToId.put(peerIDs.get(id).getPort(), id);
        }

        this.current = System.currentTimeMillis();
        this.gossipServerPort = gossipServerPort;

        this.setDaemon(true);
        try{
//            this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
            this.httpServer = HttpServer.create(new InetSocketAddress(gossipServerPort), 0);

            this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
            this.httpServer.createContext("/gossipMessages", new GossipMessages());
            this.httpServer.setExecutor(this.threadPoolExecutor);
            this.gossipLogger = initializeLogging( "GossipThreadMessages is on port-" + gossipServerPort);
            this.httpServer.start();
        } catch (IOException e){
            e.printStackTrace();
        }
        try {
            this.logger = initializeLogging(GossipThread.class.getCanonicalName()+ " on-port: " + (this.zooKeeperPeerServer.getAddress().getPort() + 2));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void shutdown(){
        this.threadPoolExecutor.shutdown();
        this.httpServer.stop(0);
        this.shutdown = true;
        this.dead = true;
        logger.fine("Shut down of Gossip thread on port: " + this.zooKeeperPeerServer.getAddress().getPort() );

    }


    @Override
    public void run() {
        Node node = new Node(this.myOwnHeartbeat, System.currentTimeMillis());
        this.nodeMap.put(this.zooKeeperPeerServer.getServerId(), node);

        int notTimeout = finalizeWait * 2;
        while (!this.shutdown){
            if (this.zooKeeperPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.LOOKING){
                continue;
            }
            sendMessageOut();
            Message message = null;

            try {
                message = this.incomingMessages.poll(notTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (message == null){
                int timeTimeout = notTimeout * 2;
                notTimeout = Math.min(timeTimeout, maxNotificationInterval);
                sendMessageOut();
            }
            else{

                if (message.getMessageType() == Message.MessageType.GOSSIP){
                    node.updateHeartbeat(this.myOwnHeartbeat);
                    node.updateTime(System.currentTimeMillis());
                    this.myOwnHeartbeat++;
                    this.logger.fine("Got this message: " + message.toString());
                    byte[] messageBytes = message.getMessageContents();
                    ByteBuffer bytes = ByteBuffer.allocate(messageBytes.length);
                    bytes.put(messageBytes);
                    bytes.flip();

                    int portSender = message.getSenderPort();
                    long IDSender = this.portToId.get(portSender);

                    Gossip gossip = new Gossip(IDSender, message.getMessageContents(), System.currentTimeMillis());

                    this.gossip.add(gossip);


                    this.gossipLogger.fine(gossip.toString());


                    while (bytes.hasRemaining()){
                        long id = bytes.getLong();
                        long heartbeat = bytes.getLong();
                        long time = bytes.getLong();
                        if (this.failedServers.contains(id) || this.cleanupedServers.contains(id)){
                            this.logger.fine("Port with id " + id + " is already dead");
                            continue;
                        }
                        if (this.zooKeeperPeerServer.getServerId() == id){
                            continue;
                        }



                        if (this.nodeMap.containsKey(id)){

                            Node pastNode = this.nodeMap.get(id);

                            if (FAIL <= (System.currentTimeMillis() - pastNode.time)){


                                this.logger.fine(this.zooKeeperPeerServer.getServerId() + ": no heartbeat from server " + id + " - server failed");
                                System.out.println(this.zooKeeperPeerServer.getServerId() + ": no heartbeat from server " + id + " - server failed");

                                this.failedServers.add(id);
                                this.zooKeeperPeerServer.addToFailed(id);
                                this.cleanupedServersMap.put(id, pastNode);
//                                this.nodeMap.remove(id);
                                if (this.peerIdsCopy.containsKey(id)) {
                                    this.ports.remove(this.peerIdsCopy.get(id).getPort());
                                }

                                if (this.zooKeeperPeerServer.getCurrentLeader().getProposedLeaderID() == id){

                                    messageLeaderFailed(id);
                                }
                                else if (this.zooKeeperPeerServer.getCurrentLeader().getProposedLeaderID() != id){
                                    messageFollowerFailed(id);
                                }


                            }
                            else if (heartbeat > this.nodeMap.get(id).getHeartBeat()){
                                this.logger.fine(this.zooKeeperPeerServer.getServerId() + ": updated " + id + "'s heartbeat sequence to " + heartbeat + " based on message from " + IDSender + " at node time " +  System.currentTimeMillis());
                                Node gotNode = this.nodeMap.get(id);
                                gotNode.updateHeartbeat(heartbeat);
                                gotNode.updateTime(System.currentTimeMillis());

                            }
                        }
                        else if (!this.failedServers.contains(id) || !cleanupedServers.contains(id)){
                            this.logger.fine(this.zooKeeperPeerServer.getServerId() + ": updated " + id + "'s heartbeat sequence to " + heartbeat + " based on message from " + IDSender + " at node time " +  System.currentTimeMillis());
//                            System.out.println(this.zooKeeperPeerServer.getServerId() + ": updated " + id + "'s heartbeat sequence to " + heartbeat + " based on message from " + IDSender + " at node time " +  System.currentTimeMillis());
                            this.nodeMap.put(id, new Node(heartbeat, System.currentTimeMillis()));
                        }
                    }
                    this.logger.fine("In my map i have: " + this.nodeMap.toString() + "and the failed nodes are these: " + this.failedServers.toString());


                    for (Long id : this.failedServers){
                        if  (CLEANUP <= System.currentTimeMillis() - this.cleanupedServersMap.get(id).getTime()){
                            this.cleanupedServers.add(id);
                        }
                    }
                    //i had to sleep a very short amount of time. Otherwise, not enough messages were being sent
                    //and servers were dieing when they should not have died.
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }


            }


        }
    }

    public void messageLeaderFailed(long id){
//        if (!this.zooKeeperPeerServer.newLeader()) {
//            this.zooKeeperPeerServer.setNewLeader(true)
            if (this.zooKeeperPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.OBSERVER) {
                this.zooKeeperPeerServer.setLeaderAccepted(false);
            } else {
                this.logger.fine(this.zooKeeperPeerServer.getServerId() + ": switching from FOLLOWING to LOOKING");
                System.out.println(this.zooKeeperPeerServer.getServerId() + ": switching from FOLLOWING to LOOKING");
                this.zooKeeperPeerServer.removeLeader();
            }
//        }


    }

    public void messageFollowerFailed(long id){

        this.zooKeeperPeerServer.serverFailed(id);

    }

    public void sendMessageOut(){
        while(this.ports.size() > 0 && this.ports.peek() == this.zooKeeperPeerServer.getAddress().getPort()){
            this.ports.poll();
        }
        if (this.ports.size() <= 0){
            return;
        }
        int port = this.ports.poll();
        this.ports.offer(port);
        //need to do *24 because each message is 24 bytes (3 longs)
        int messageSize = this.nodeMap.size() * 24;
        ByteBuffer byteBuffer = ByteBuffer.allocate(messageSize);
        for (Long id: this.nodeMap.keySet()){
            Node node = this.nodeMap.get(id);
            long heartbeat = node.heartBeat;
            long time = node.time;
            byteBuffer.putLong(id);
            byteBuffer.putLong(heartbeat);
            byteBuffer.putLong(time);
        }
        byte[] message = byteBuffer.array();
        this.zooKeeperPeerServer.sendMessage(Message.MessageType.GOSSIP, message, new InetSocketAddress("localhost", port));
    }





    public boolean checkLeaderFailed(long id){
        if (this.failedServers.contains(id) || this.cleanupedServers.contains(id)){
            return true;
        }
        return false;
    }

    public boolean isDead(){
        if (dead) {
            return true;
        }
        return false;
    }

    public class Node{
        private long heartBeat;
        private long time;

        public Node(long heartBeat, long time){
            this.heartBeat = heartBeat;
            this.time = time;
        }
        @Override
        public String toString() {
            return "Data{" +
                    "heartBeat=" + heartBeat +
                    ", time=" + time +
                    '}';
        }

        public long getHeartBeat(){
            return this.heartBeat;
        }
        public long getTime(){
            return this.time;
        }
        public void updateHeartbeat(long newHeartbeat){
            this.heartBeat = newHeartbeat;
        }
        public void updateTime(long newTime){
            this.time = newTime;
        }
    }


    public class GossipMessages implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            OutputStream outputStream =t.getResponseBody();
            StringBuilder stringBuilder = new StringBuilder();
            for (Gossip m: gossip)
            {
                stringBuilder.append(m.toString()).append("\n");
            }
            byte[] returnString = stringBuilder.toString().getBytes();
            t.sendResponseHeaders(200, returnString.length);
            outputStream.write(returnString);
            outputStream.close();
        }
    }


    public class Gossip{
        private long receiverID;
        private  byte[] messageReceived;
        private long time;

        @Override
        public String toString() {
            return "Gossip{" +
                    "receiverID=" + receiverID +
                    ", messageReceived=" + Arrays.toString(messageReceived) +
                    ", time=" + time +
                    '}';
        }

        public Gossip(long receiverID, byte[] messageReceived, long time) {
            this.receiverID = receiverID;
            this.messageReceived = messageReceived;
            this.time = time;
        }

    }

}






