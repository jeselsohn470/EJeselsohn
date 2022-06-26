package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.stage5.Client;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Vote;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class TestScript {

    private ZooKeeperPeerServer zooKeeperPeerServer;
    private String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";

    private String validClass2 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world Jonathan!\";\n    }\n}\n";

    private String badClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public int run()\n    {\n        return \"Hello world!\";\n    }\n}\n";
    private String mathClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public int run()\n    {\n        return 78 ;\n    }\n}\n";

    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private int[] ports = {8000, 8010, 8020, 8030, 8040, 8050, 8060};
    //private int[] ports = {8010, 8020};
    private int leaderPort = this.ports[this.ports.length - 1];
    private int observerPort = 8800;
    private int httpServer = 8080;
    private int clientPort = 2600;
    //private InetSocketAddress myAddress = new InetSocketAddress("localhost", this.myPort);
    private ArrayList<ZooKeeperPeerServer> servers;
    private GatewayServer gatewayServer;
    private GatewayPeerServerImpl gatewayPeerServer;
    private List<Integer> observers = new ArrayList<>();
    private Long myID;

    public TestScript(Long id) throws Exception {
        this.myID = id;
        //step 1: create sender & sending queue
        //step 2: create servers
        createServers();

        //step2.1: wait for servers to get started
//		try {
//			Thread.sleep(3900);
//		}
//		catch (Exception e) {
//		}

//		//printLeaders();
//		Client client = new ClientImpl("localhost", this.httpServer);
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 1\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 2\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 3\";\n    }\n}\n");
////		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 4\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 5\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 6\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 7\";\n    }\n}\n");
////		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 8\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 9\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 10\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 11\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 12\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 13\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 14\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 15\";\n    }\n}\n");
//		client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 16\";\n    }\n}\n");

//		Client client2 = new ClientImpl("localhost", 7685);
//		Message message2 = sendMessage(mathClass);
//		client2.sendCompileAndRunRequest(message2.getNetworkPayload());

        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
//		for (int i = 0; i < this.ports.length; i++) {
//			String code = this.validClass.replace("world!", "world! from code version " + i);
//
//		}
//		System.out.println("This is the response to be returned: " + client.getResponse().getBody());
//		System.out.println("This is the response to be returned: " + client.getResponse().getBody());
//		System.out.println("This is the response to be returned: " + client.getResponse().getBody());
////		System.out.println("This is the response to be returned: " + client.getResponse().getBody());
//		System.out.println("This is the response to be returned: " + client.getResponse().getBody());
//		System.out.println("This is the response to be returned: " + client.getResponse().getBody());
//		System.out.println("This is the response to be returned: " + client.getResponse().getBody());





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
        //stopServers();
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
        if (myID == 6L){
            zooKeeperPeerServer.shutdown();
        }
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Have all the threads have enough time to elect a new leader");
        try {
            Thread.sleep(3900);
        }
        catch (Exception e) {
        }
        //printLeaders();
        if(myID != 6L && myID != 15L)
        {
            zooKeeperPeerServer.shutdown();
        }
        else if (myID == 15L)
        {
            gatewayServer.stop();
        }

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
        if(myID == 15L ){
            gatewayServer = new GatewayServer(httpServer, 15L, peerIDtoAddress, observers);
        }
        else
        {
            zooKeeperPeerServer = new ZooKeeperPeerServerImpl(ports[Math.toIntExact(myID)], 0, myID, peerIDtoAddress, observers);
            new Thread((Runnable) zooKeeperPeerServer, "Server on port " + zooKeeperPeerServer.getAddress().getPort()).start();
        }

//		//create servers
//		this.servers = new ArrayList<>(3);
//		for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
//			HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
//			if(entry.getKey() == 15L){
//				map.remove(entry.getKey());
//				//GatewayPeerServerImpl server = new GatewayPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
//				//this.servers.add(server);
//
////				new Thread((Runnable) server, "Server on port " + server.getAddress().getPort()).start();
//			}
//			else
//			{
//				map.remove(entry.getKey());
//				this.servers.add(server);
//			}
//		}
    }
    public static void main(String[] args) {
        try {
            new TestScript(Long.parseLong(args[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}