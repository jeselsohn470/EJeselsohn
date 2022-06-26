package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Stage5PeerServerDemo {
    private String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";
    private String badClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public int run()\n    {\n        return \"Hello world!\";\n    }\n}\n";
    private String mathClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public int run()\n    {\n        return 78 ;\n    }\n}\n";

    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private int[] ports = {8000, 8010, 8020, 8030, 8040, 8050, 8060};
    //private int[] ports = {8010, 8020};
    private int leaderPort = this.ports[this.ports.length - 1];
    private int observerPort = 8800;
    private int httpServer = 9888;
    private int clientPort = 2600;
    //private InetSocketAddress myAddress = new InetSocketAddress("localhost", this.myPort);
    private ArrayList<ZooKeeperPeerServer> servers;
    private GatewayServer gatewayServer;
    private GatewayPeerServerImpl gatewayPeerServer;
    private  List<Integer> observers = new LinkedList<>();
    Client client = new ClientImpl("localhost", this.httpServer);

    public Stage5PeerServerDemo() throws Exception {
        //step 1: create sender & sending queue
        //step 2: create servers
        createServers();
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
        }

        printLeaders();

        Message message1 = sendMessage(validClass);





//		Message message = sendMessage(badClass);
		client.sendCompileAndRunRequest(validClass);
////		Client client2 = new ClientImpl("localhost", 7685);
////		Message message2 = sendMessage(mathClass);
////		client2.sendCompileAndRunRequest(message2.getNetworkPayload());
//
//		//step 3: since we know who will win the election, send requests to the leader, this.leaderPort
////		for (int i = 0; i < this.ports.length; i++) {
////			String code = this.validClass.replace("world!", "world! from code version " + i);
////
////		}
//		System.out.println("This is the response to be returned: " + client.getResponse().getBody());
//
//		Message message3 = sendMessage(mathClass);
//		client.sendCompileAndRunRequest(message3.getNetworkPayload());





//		Client client2 = new ClientImpl("localhost", 7685);
//		Message message2 = sendMessage(mathClass);
//		client2.sendCompileAndRunRequest(message2.getNetworkPayload());

        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
//		for (int i = 0; i < this.ports.length; i++) {
//			String code = this.validClass.replace("world!", "world! from code version " + i);
//
//		}
        //System.out.println("This is the response to be returned: " + client.getResponse().getBody());



//		Message message5 = sendMessage(validClass);
//		client.sendCompileAndRunRequest(message5.getNetworkPayload());
////		Client client1 = new ClientImpl("localhost", 8876);
//		System.out.println("This is the response to be : " + client.getResponse().getBody());
//		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());
//
//		Client clientNew = new ClientImpl("localhost", this.httpServer);
//		Message message8 = sendMessage(validClass);
//		clientNew.sendCompileAndRunRequest(message8.getNetworkPayload());
//		System.out.println("I am a different client " + clientNew.getResponse().getBody());
//
//		Message message6 = sendMessage(badClass);
//		client.sendCompileAndRunRequest(message6.getNetworkPayload());
////		Client client1 = new ClientImpl("localhost", 8876);
//		System.out.println("This is the response to be : " + client.getResponse().getBody());
//		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());
//
//		Message message7 = sendMessage(mathClass);
//		client.sendCompileAndRunRequest(message7.getNetworkPayload());
////		Client client1 = new ClientImpl("localhost", 8876);
//		System.out.println("This is the response to be : " + client.getResponse().getBody());
//		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());
//
//		Message message9 = sendMessage(validClass);
//		client.sendCompileAndRunRequest(message9.getNetworkPayload());
////		Client client1 = new ClientImpl("localhost", 8876);
//		System.out.println("This is the response to be : " + client.getResponse().getBody());
//		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());
//
//		Message message10 = sendMessage(mathClass);
//		clientNew.sendCompileAndRunRequest(message10.getNetworkPayload());
//		System.out.println("I am a different client ++" + clientNew.getResponse().getBody());

//		System.out.println("This is the response to be returned: " + client1.getResponse().getBody());
//
//		System.out.println("This is the response to be returned: " + client2.getResponse().getBody());

        //step 4: validate responses from leader

        //
        // printResponses();

        //step 5: stop servers
        stopServers();
    }

    private void printLeaders() {
        for (ZooKeeperPeerServer server : this.servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            }
        }
    }

    private void stopServers() {
        //could it be 1 is deleted before 0 and then always returns false???
            this.servers.get(6).shutdown();
            this.servers.remove(this.servers.get(6));



            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        printLeaders();

        try{
		Message message5 = sendMessage(validClass);
		client.sendCompileAndRunRequest(validClass);
//		Client client1 = new ClientImpl("localhost", 8876);
		System.out.println("This is the response to be : " + client.getResponse().getBody());
		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());

		Client clientNew = new ClientImpl("localhost", this.httpServer);
		Message message8 = sendMessage(validClass);
		clientNew.sendCompileAndRunRequest(validClass);
		System.out.println("I am a different client " + clientNew.getResponse().getBody());

//		Message message6 = sendMessage(badClass);
//		client.sendCompileAndRunRequest(badClass);
////		Client client1 = new ClientImpl("localhost", 8876);
//		System.out.println("This is the response to be : " + client.getResponse().getBody());
////		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());
//
//		client.sendCompileAndRunRequest(mathClass);
////		Client client1 = new ClientImpl("localhost", 8876);
//		System.out.println("This is the response to be : " + client.getResponse().getBody());
////		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());
//
//		Message message9 = sendMessage(validClass);
//		client.sendCompileAndRunRequest(validClass);
////		Client client1 = new ClientImpl("localhost", 8876);
//		System.out.println("This is the response to be : " + client.getResponse().getBody());
//		System.out.println("This is the response number to be returned: " + client.getResponse().getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();
        }
        gatewayServer.stop();

    }

//	private void printResponses() throws Exception {
//		String completeResponse = "";
//		for (int i = 0; i < this.ports.length; i++) {
//			Message msg = this.incomingMessages.take();
//			String response = new String(msg.getMessageContents());
//			completeResponse += "Response #" + i + ":\n" + response + "\n";
//		}
//		System.out.println(completeResponse);
//	}

    private Message sendMessage(String code) throws InterruptedException {
        return new Message(Message.MessageType.WORK, code.getBytes(), "localhost", clientPort, "localhost", this.httpServer);
    }

    private void createServers() throws IOException {
        //create IDs and addresses
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(8);
        for (int i = 0; i < 7; i++) {
            peerIDtoAddress.put(Integer.valueOf(i).longValue(), new InetSocketAddress("localhost", this.ports[i]));
        }
        peerIDtoAddress.put(Integer.valueOf(15).longValue(), new InetSocketAddress("localhost", httpServer+2));
        observers.add(httpServer+2);

        //create servers
        this.servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            if(entry.getKey() == 15L){
                map.remove(entry.getKey());
                //GatewayPeerServerImpl server = new GatewayPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
                //this.servers.add(server);
                gatewayServer = new GatewayServer(httpServer, 15L, map,  observers);
//				new Thread((Runnable) server, "Server on port " + server.getAddress().getPort()).start();
            }
            else
            {
                map.remove(entry.getKey());
                ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map, observers);
                this.servers.add(server);
                new Thread((Runnable) server, "Server on port " + server.getAddress().getPort()).start();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Stage5PeerServerDemo();
    }
}
