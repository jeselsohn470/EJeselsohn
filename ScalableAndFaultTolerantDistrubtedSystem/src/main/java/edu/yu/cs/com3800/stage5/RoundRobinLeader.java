

package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class RoundRobinLeader extends Thread implements LoggingServer{


    private ConcurrentLinkedDeque<InetSocketAddress> workers = new ConcurrentLinkedDeque<>();
    private boolean shutdown = false;
    private InetSocketAddress myAddress;
    private Logger logger;
    private ExecutorService threadPool = Executors.newFixedThreadPool(4);
    private List<Integer> observerPorts;
    private ZooKeeperPeerServerImpl zooKeeperPeerServer;
    private ConcurrentHashMap<Long, List<byte[]>> currentJobs = new ConcurrentHashMap<>();
    private Set<Long> failedPorts = new HashSet<>();
    private Socket socket;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<byte[], OutputStream> connectionWithGateway = new ConcurrentHashMap<>();
    private Map<Integer, Long> portToId = new HashMap<>();
    private Map<Long,InetSocketAddress> peerIDtoAddress;
    private LinkedBlockingQueue<Message> messages;
    private GossipThread gossipThread;
    private ConcurrentHashMap<Long, Message> idToCompletedWord = new ConcurrentHashMap<>();
    private Message oldWork;
    private int gossipServerPort;




    public RoundRobinLeader(ZooKeeperPeerServerImpl zooKeeperPeerServer, InetSocketAddress myAddress, Map<Long,InetSocketAddress> peerIDtoAddress, LinkedBlockingQueue<Message> messages, List<Integer> observerPorts, Message oldWork, int gossipServerPort){
        this.myAddress = myAddress;
        this.observerPorts = observerPorts;
        this.messages = messages;
        this.gossipServerPort = gossipServerPort;
        this.workers.addAll(peerIDtoAddress.values());
        for (InetSocketAddress port : this.workers){
            if (this.observerPorts.contains(port.getPort())){
                this.workers.remove(port);
            }
        }
        this.oldWork = oldWork;
        if (this.oldWork != null){
            idToCompletedWord.put(oldWork.getRequestID(), oldWork);
        }

        for (Long id: peerIDtoAddress.keySet()){
            this.portToId.put(peerIDtoAddress.get(id).getPort(), id);
        }
        this.setDaemon(true);

        this.zooKeeperPeerServer = zooKeeperPeerServer;
        try {
            this.logger = initializeLogging(RoundRobinLeader.class.getCanonicalName()+ " on-port: " + (this.myAddress.getPort() + 2));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.peerIDtoAddress = peerIDtoAddress;
        this.gossipThread = new GossipThread(this.zooKeeperPeerServer, this.messages, this. peerIDtoAddress, this.observerPorts, gossipServerPort);
        this.gossipThread.start();
        this.setDaemon(true);
        //if the leader died
        if (this.zooKeeperPeerServer.didLeaderDie()) {
            getAllMessagesFromFollowers();
        }
    }

    public void shutdown(){
        this.threadPool.shutdown();
        this.workers.clear();
        this.gossipThread.shutdown();
//        this.gossipThread.interrupt();
        this.logger.fine("RRL shutdown");
        interrupt();
        this.shutdown = true;
//        if  (this.serverSocket != null){
//            try {
//                this.serverSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

    }

//    Gathers any work that it, or other nodes, completed as workers after the previous leader died.
//    Eventually the gateway will send a request with the same request ID to the leader,
//    and the leader will simply reply with the already-completed work instead of having
//    a follower redo the work from scratch.
    private void getAllMessagesFromFollowers() {

        InetSocketAddress inetSocketAddress = this.workers.peek();

        //this ensures no observer gets work assigned to it. And no leader will get work because we removed him

        while (this.observerPorts.contains(inetSocketAddress.getPort()) || this.failedPorts.contains(this.portToId.get(inetSocketAddress.getPort()))){
            this.workers.poll();
            inetSocketAddress = this.workers.peek();
        }
        int workerSize = workers.size();
        for(int i = 0 ; i< workerSize; i++){
            InetSocketAddress currentWorker = getWorker();
            Socket newSocket = null;
            try {
                newSocket = new Socket(currentWorker.getHostName(), currentWorker.getPort()+2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //create the output stream which will connect to the Java Runner Follower
            OutputStream osToFollower = null;
            InputStream in = null;
            try {
                osToFollower = newSocket.getOutputStream();
                in  = newSocket.getInputStream();
                osToFollower.write(requestMessage().getNetworkPayload());
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] followerReturned = Util.readAllBytesFromNetwork(in);
            Message currentM = new Message(followerReturned);
            if (currentM.getRequestID() == -1){
                continue;
            }
            idToCompletedWord.put(currentM.getRequestID(), currentM);
        }

    }
    private Message requestMessage(){
        return new Message(Message.MessageType.NEW_LEADER_GETTING_LAST_WORK, new byte[0], "", 0, "", 0);
    }
    @Override
    public void run() {
        Message message = null;
        try {
            this.logger.fine("started the RRL on port: " + this.myAddress.getPort() + 2);
            this.serverSocket = new ServerSocket(this.myAddress.getPort() + 2);
            byte[] sendThis;

            while (!this.shutdown) {
                //waiting for a message to be added to the queue
                this.socket = this.serverSocket.accept();
                this.logger.fine("connection with GatewayServer on port: " + this.socket.getPort());


                InetSocketAddress worker = getWorker();

                InputStream inputStream = this.socket.getInputStream();
                 sendThis = Util.readAllBytesFromNetwork(inputStream);

                 //see if the gateway is trying to get old work
                Message currentMessage = new Message(sendThis);
                this.connectionWithGateway.put(sendThis, this.socket.getOutputStream());


                 long id = this.portToId.get(worker.getPort());
                if (!this.currentJobs.containsKey(id)){
                    List<byte[]> byteList = new ArrayList<>();
                    byteList.add(sendThis);
                    this.currentJobs.put(id, byteList);
                }
                else{
                    List<byte[]> byteList = this.currentJobs.get(id);
                    byteList.add(sendThis);
                    this.currentJobs.put(id, byteList);
                }

//                this.connectionWithGateway.put(sendThis, this.socket.getOutputStream());

                this.threadPool.execute(new TCPServer(this.connectionWithGateway.get(sendThis), worker, sendThis));



            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //this method needs to remove the followers and reassigns the work
    //reassign any client request work it had given the dead node to a different node
    public void failedServer(long id){
        this.failedPorts.add(id);
        if (this.currentJobs.containsKey(id)){
            List<byte[]> jobs = this.currentJobs.get(id);
            for (byte[] job : jobs){
                InetSocketAddress worker = getWorker();
                int p = worker.getPort();
                long i = this.portToId.get(p);

                OutputStream outputStream = this.connectionWithGateway.get(job);
                this.threadPool.execute(new TCPServer(outputStream, worker, job));


            }
        }

    }

    //this method tells us which worker to send the message to and ensures we do not send it to the Gateway Server
    private InetSocketAddress getWorker(){

        //take the first worker off the queue, then add it to the end of the queue so it will get recycled
        InetSocketAddress inetSocketAddress = this.workers.peek();
        //this ensures no observer gets work assigned to it. And no leader will get work because we removed him
        while (this.observerPorts.contains(inetSocketAddress.getPort()) || this.failedPorts.contains(this.portToId.get(inetSocketAddress.getPort()))){
            this.workers.poll();
            inetSocketAddress = this.workers.peek();
        }
        InetSocketAddress worker = this.workers.poll();

        this.workers.offer(worker);
        return worker;
    }

    public class TCPServer extends Thread{

//        private Socket socket;
        private OutputStream outputStream;
        private InetSocketAddress inetSocketAddress;
        private byte[] byteArray;

        public TCPServer(OutputStream outputStream, InetSocketAddress inetSocketAddress, byte[] byteArray) {
//            this.socket = socket;
            this.outputStream = outputStream;
            this.inetSocketAddress = inetSocketAddress;
            this.byteArray = byteArray;
            this.setDaemon(true);

        }

        @Override
        public void run(){
            try {


                //message to be sent to the Follower
                Message messageToFollower = new Message(this.byteArray);
                logger.fine("message from RRL sent to Java Runner Follower: "+ messageToFollower.toString());

                //connected to follower
                Socket newSocket = new Socket(this.inetSocketAddress.getHostName(), (this.inetSocketAddress.getPort() + 2));
                //create the output stream which will connect to the Java Runner Follower
                OutputStream osToFollower = newSocket.getOutputStream();
                //pass it to the JRF
                osToFollower.write(messageToFollower.getNetworkPayload());
                logger.fine("connected with JRF on port: "+ newSocket.getPort());

                //getting back from follower
                InputStream inputStreamFollower = newSocket.getInputStream();
                byte[] followerReturned = Util.readAllBytesFromNetwork(inputStreamFollower);

                //output stream which will connect to the gateway
//                OutputStream osToGateway = this.socket.getOutputStream();
                Message m = new Message(followerReturned);

                this.outputStream.write(followerReturned);
                long id  = portToId.get(this.inetSocketAddress.getPort());
                List<byte[]> listOfJobs = currentJobs.get(id);
                listOfJobs.remove(byteArray);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

