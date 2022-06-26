package edu.yu.cs.com3800.stage5;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GatewayServer implements LoggingServer {
    private int port;
    private HttpServer server;
    private GatewayPeerServerImpl gatewayPeerServer;
    private ThreadPoolExecutor threadPoolExecutor;
    private Map<Long, InetSocketAddress> peerIDToAddress = new HashMap<>();
    private Logger logger;
    private ConcurrentHashMap<Message, HttpExchange> workToClient = new ConcurrentHashMap<>();
    private AtomicLong requestID = new AtomicLong(0);
    private Long id;

    //i pass in the servers list so that i can print the observers status as well. its just for testing purposes.
    public GatewayServer(int port, Long id, Map<Long, InetSocketAddress> peerIDToAddress, List<Integer> observerPorts) throws IOException {


        this.port = port;
        this.peerIDToAddress = peerIDToAddress;

        this.gatewayPeerServer = new GatewayPeerServerImpl(this.port + 2,0,  id, peerIDToAddress, observerPorts);
        this.gatewayPeerServer.start();

        this.id = id;

        //this is done just to print the observer id, its leader and its status



//
        this.server = HttpServer.create(new InetSocketAddress(this.port), 0);

        this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

        this.server.createContext("/compileandrun", new MyHandler());
        this.server.setExecutor(this.threadPoolExecutor); // creates the threadPoolExecutor




        try {
            this.logger = initializeLogging(GatewayServer.class.getCanonicalName()+ "on-port: " + this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.logger.fine("servers started");
        start();

    }


    public void start() {
        this.server.start();
        this.logger.fine("Gateway servers started");
    }

    public void stop() {
        this.gatewayPeerServer.shutdown();
        this.threadPoolExecutor.shutdown();
        this.server.stop(0);
        this.logger.fine("Gateway servers stopped");

    }

    public boolean leaderAlive(){
        return this.gatewayPeerServer.leaderALive();
    }
    public Long leaderID(){
        return this.gatewayPeerServer.getCurrentLeader().getProposedLeaderID();
    }


    private InetSocketAddress getLeader(){
        return this.gatewayPeerServer.getLeader();
    }

    private class MyHandler implements HttpHandler {
        private boolean changeMessage = false;
        @Override
        public void handle(HttpExchange t) throws IOException {


            //add different headers
            //leader is chosen
            //full list of nodes


            logger.fine("set http connection");
            Headers headers = t.getRequestHeaders();
            String contentType = headers.get("Content-Type").get(0);
            String command = t.getRequestMethod();
            int statusCode = 200;
            byte[] returnBytes;


            //this section is for the http connection about leader election
            if (contentType.equals("hasLeader")){
                if (leaderAlive()){
                    OutputStream outputStream1 = t.getResponseBody();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Long i : peerIDToAddress.keySet()){
                        if (i == leaderID()) {
                            stringBuilder.append("Server whose ID is ").append(i).append(" is in the state of LEADING \n");
                        }
                        else if (i != id){
                            stringBuilder.append("Server whose ID is ").append(i).append(" is in the state of FOLLOWING \n");
                        }
                    }

                    stringBuilder.append("Server whose ID is ").append(id).append(" is in the state of OBSERVER \n");

                    String answer = stringBuilder.toString();
                    t.sendResponseHeaders(statusCode, answer.getBytes().length);
                    outputStream1.write(answer.getBytes());
                    outputStream1.close();
                    return;
                }
                else{
                    OutputStream outputStream1 = t.getResponseBody();
                    String leaderElected = "leader not elected";
                    t.sendResponseHeaders(statusCode, leaderElected.getBytes().length);
                    outputStream1.write(leaderElected.getBytes());
                    outputStream1.close();
                    return;
                }

            }


            //check if content type is correct
            if (!contentType.equals("text/x-java-source")){
                t.sendResponseHeaders(400, -1);
                t.getRequestBody();
                OutputStream os = t.getResponseBody();
                os.close();
                logger.fine("contentType was not text/x-java-source and therefore returned a 400");
                return;
            }
            //not sure i need this or not
            if (!command.equals("POST")){
                t.sendResponseHeaders(405,-1);
                t.getRequestBody();
                OutputStream os = t.getResponseBody();
                os.close();
                logger.info("Command was not a post and therefore returned a 405");
                return;
            }
            ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
            InputStream is= t.getRequestBody();
            is.transferTo(baOutputStream);
            InputStream copyIS = new ByteArrayInputStream(baOutputStream.toByteArray());
            byte[] readFromNetwork  = Util.readAllBytesFromNetwork(copyIS);

//            Message message = new Message(Message.MessageType.WORK, readFromNetwork, "localhost", port, "localhost", getLeader().getPort() + 2, requestID.getAndIncrement());



            //checking if the leader died
            while (gatewayPeerServer.isLeaderDead()){
                this.changeMessage = true;
            }
            //if the leader died, need to collect all the unreturned work and resend it out
            //locally queue all new client requests until a new leader is elected
            if (changeMessage){
                this.changeMessage = false;
                for (Message mess : workToClient.keySet()){
                    HttpExchange httpExchange = workToClient.get(mess);
                    Message message1 = new Message(Message.MessageType.NEW_LEADER_GETTING_LAST_WORK, mess.getMessageContents(), mess.getSenderHost(), mess.getSenderPort(), mess.getReceiverHost(), mess.getReceiverPort(), mess.getRequestID());
                    workToClient.put(message1, httpExchange);
                    workToClient.remove(mess);
                }

            }


            InetSocketAddress leader = getLeader();


            Message outgoingMessage = createMessage(readFromNetwork, leader);

            workToClient.put(outgoingMessage, t);
            Socket clientSocket = null;
            while (true) {

                //will go into the catch if the leader dies and cant make a connection.
                //wait that amount of time because thats the amount of time
                //it took my computer to elect a new leader
                try {
                    clientSocket = new Socket(leader.getHostName(), leader.getPort() + 2);
                    break;
                } catch (IOException e) {
                    try {
                        Thread.sleep(68000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    leader = getLeader();
                    outgoingMessage = new Message(Message.MessageType.NEW_LEADER_GETTING_LAST_WORK, outgoingMessage.getMessageContents(), "localhost", outgoingMessage.getSenderPort(), outgoingMessage.getReceiverHost(), outgoingMessage.getReceiverPort(), outgoingMessage.getRequestID());
                    changeMessage = true;
                }
            }

            logger.fine("Connected to RRL on port:  " + leader.getPort() +2);
            OutputStream outputStream = clientSocket.getOutputStream();


            logger.fine("GatewayServer: Message read from network " + outgoingMessage.toString());


            outputStream.write(outgoingMessage.getNetworkPayload());
            logger.fine("Sent message to the RR Leader");


//            if (gatewayPeerServer.isLeaderDead()){
//                this.changeMessage = true;
//                return;
//            }
            //Get data back from the leader
            InputStream inputStreamFromLeader = clientSocket.getInputStream();
            returnBytes = Util.readAllBytesFromNetwork(inputStreamFromLeader);

            //GET the message from the input stream


            Message m = new Message(returnBytes);

            logger.fine("GatewayServer: Message received from the RR leader " +m.toString());
            if (m.getErrorOccurred()) {
                statusCode = 400;
                logger.fine("The java runner had an issue running the code so http will return an error " + "code of 400");

            }
            //send it back to the client
            OutputStream outputStream1 = t.getResponseBody();
            t.sendResponseHeaders(statusCode, m.getMessageContents().length);
            outputStream1.write(m.getMessageContents());
            workToClient.remove(outgoingMessage);
            logger.fine("Sent the information back to the client");

            outputStream1.close();
            t.close();
        }



    }

    public Message createMessage(byte[] code, InetSocketAddress inetSocketAddress) {
        Message message =  new Message(Message.MessageType.WORK, code, "localhost", port + 2, inetSocketAddress.getHostName(), inetSocketAddress.getPort()+2, requestID.getAndIncrement());
        return message;
    }

}


