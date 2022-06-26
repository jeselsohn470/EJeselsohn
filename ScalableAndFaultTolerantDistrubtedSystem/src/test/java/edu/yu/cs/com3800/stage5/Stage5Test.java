package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Vote;
import edu.yu.cs.com3800.ZooKeeperPeerServer;
import edu.yu.cs.com3800.stage5.GatewayPeerServerImpl;
import edu.yu.cs.com3800.stage5.GatewayServer;
import edu.yu.cs.com3800.stage5.Stage5PeerServerDemo;
import edu.yu.cs.com3800.stage5.ZooKeeperPeerServerImpl;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class Stage5Test {


    @Test
    public void testWorkerDies(){
        String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world1!\";\n    }\n}\n";
        String invalidClass2 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public int run()\n    {\n        return \"Hello world2!\";\n    }\n}\n";
        String validClass3 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world3!\";\n    }\n}\n";
        String validClass4 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world4!\";\n    }\n}\n";
        String validClass5 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world5!\";\n    }\n}\n";
        String validClass6 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world6!\";\n    }\n}\n";
        String validClass7 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world7!\";\n    }\n}\n";
        String invalidClass8 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public int run()\n    {\n        return \"Hello world8!\";\n    }\n}\n";



        int[] ports = {6010, 6020, 6030, 6040, 6050, 6060};

        int httpServerPort = 6080;
        GatewayPeerServerImpl gatewayPeerServer;
        ArrayList<ZooKeeperPeerServer> servers;
        List<Integer> observerPorts = new ArrayList<>();
        observerPorts.add(httpServerPort + 2);

        //step 1: create sender & sending queue
        //step 2: create servers
//        createServers();
        //create IDs and addresses
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(8);
        for (int i = 0; i < 6; i++) {
            peerIDtoAddress.put(Integer.valueOf(i).longValue(), new InetSocketAddress("localhost", ports[i]));
        }
        peerIDtoAddress.put(Integer.valueOf(18).longValue(), new InetSocketAddress("localhost", httpServerPort+2));


        //create servers
        GatewayServer gatewayServer = null;
        servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            if(entry.getKey() == 18){
                map.remove(entry.getKey());
                try {
                    gatewayServer = new GatewayServer(httpServerPort, 18L, map, observerPorts);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                map.remove(entry.getKey());
                ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map, observerPorts);
                servers.add(server);
                new Thread((Runnable) server, "Server on port " + server.getAddress().getPort()).start();
            }
        }
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
//        printLeaders();
        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            }
        }
        Client client = null;
        try {
            client = new ClientImpl("localhost", httpServerPort);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            client.sendCompileAndRunRequest(validClass);
            Client.Response r1 = client.getResponse();
            System.out.println("This is the response to be returned: " + r1.getBody());
            System.out.println("This is the status code to be returned: " + r1.getCode());
            //step 4: validate responses from leader

            client.sendCompileAndRunRequest(invalidClass2);

            Client.Response r2 = client.getResponse();
            System.out.println("This is the response to be returned: " + r2.getBody());
            System.out.println("This is the status code to be returned: " + r2.getCode());
//        printResponses();

            client.sendCompileAndRunRequest(validClass3);

            Client.Response r3 = client.getResponse();
            System.out.println("This is the response to be returned: " + r3.getBody());
            System.out.println("This is the status code to be returned: " + r3.getCode());

            client.sendCompileAndRunRequest(validClass4);

            Client.Response r4 = client.getResponse();
            System.out.println("This is the response to be returned: " + r4.getBody());
            System.out.println("This is the status code to be returned: " + r4.getCode());


            client.sendCompileAndRunRequest(validClass5);

            Client.Response r5 = client.getResponse();
            System.out.println("This is the response to be returned: " + r5.getBody());
            System.out.println("This is the status code to be returned: " + r5.getCode());

        }
        catch(Exception e){
            e.printStackTrace();
        }

        //kill a follower, print the leaders and send more requests
        System.out.println("killing follower with id 0");
        servers.get(0).shutdown();
        servers.remove(servers.get(0));



        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());

            }
        }

        try {
            client.sendCompileAndRunRequest(validClass6);

            Client.Response r6 = client.getResponse();
            System.out.println("This is the response to be returned: " + r6.getBody());
            System.out.println("This is the status code to be returned: " + r6.getCode());

            client.sendCompileAndRunRequest(validClass7);

            Client.Response r7 = client.getResponse();
            System.out.println("This is the response to be returned: " + r7.getBody());
            System.out.println("This is the status code to be returned: " + r7.getCode());

            client.sendCompileAndRunRequest(invalidClass8);

            Client.Response r8 = client.getResponse();
            System.out.println("This is the response to be returned: " + r8.getBody());
            System.out.println("This is the status code to be returned: " + r8.getCode());
        }catch(Exception e){
            e.printStackTrace();
        }

        //step 5: stop servers
//        stopServers();
        for (ZooKeeperPeerServer server : servers) {
            server.shutdown();
        }
        gatewayServer.stop();

    }

    @Test
    public void testLeaderDies(){
        String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world1!\";\n    }\n}\n";
        String validClass2 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world2!\";\n    }\n}\n";
        String validClass3 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world3!\";\n    }\n}\n";
        String validClass4 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world4!\";\n    }\n}\n";
        String invalidClass5 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public int run()\n    {\n        return \"Hello world5!\";\n    }\n}\n";
        String validClass6 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world6!\";\n    }\n}\n";
        String validClass7 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world7!\";\n    }\n}\n";
        String validClass8 = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world8!\";\n    }\n}\n";



        int[] ports = {7010, 7020, 7030, 7040, 7050, 7060};

        int httpServerPort = 7080;
        GatewayPeerServerImpl gatewayPeerServer;
        ArrayList<ZooKeeperPeerServer> servers;
        List<Integer> observerPorts = new ArrayList<>();
        observerPorts.add(httpServerPort + 2);

        //step 1: create sender & sending queue
        //step 2: create servers
//        createServers();
        //create IDs and addresses
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(8);
        for (int i = 0; i < 6; i++) {
            peerIDtoAddress.put(Integer.valueOf(i).longValue(), new InetSocketAddress("localhost", ports[i]));
        }
        peerIDtoAddress.put(Integer.valueOf(18).longValue(), new InetSocketAddress("localhost", httpServerPort+2));


        //create servers
        GatewayServer gatewayServer = null;
        servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            if(entry.getKey() == 18){
                map.remove(entry.getKey());
                try {
                    gatewayServer = new GatewayServer(httpServerPort, 18L, map, observerPorts);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                map.remove(entry.getKey());
                ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map, observerPorts);
                servers.add(server);
                new Thread((Runnable) server, "Server on port " + server.getAddress().getPort()).start();
            }
        }
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
//        printLeaders();
        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            }
        }
        Client client = null;
        try {
            client = new ClientImpl("localhost", httpServerPort);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            client.sendCompileAndRunRequest(validClass);

            Client.Response r1 = client.getResponse();
            System.out.println("This is the response to be returned: " + r1.getBody());
            System.out.println("This is the status code to be returned: " + r1.getCode());
            //step 4: validate responses from leader

            client.sendCompileAndRunRequest(validClass2);

            Client.Response r2 = client.getResponse();
            System.out.println("This is the response to be returned: " + r2.getBody());
            System.out.println("This is the status code to be returned: " + r2.getCode());//        printResponses();

            client.sendCompileAndRunRequest(validClass3);

            Client.Response r3 = client.getResponse();
            System.out.println("This is the response to be returned: " + r3.getBody());
            System.out.println("This is the status code to be returned: " + r3.getCode());

            client.sendCompileAndRunRequest(validClass4);

            Client.Response r4 = client.getResponse();
            System.out.println("This is the response to be returned: " + r4.getBody());
            System.out.println("This is the status code to be returned: " + r4.getCode());

            client.sendCompileAndRunRequest(invalidClass5);

            Client.Response r5 = client.getResponse();
            System.out.println("This is the response to be returned: " + r5.getBody());
            System.out.println("This is the status code to be returned: " + r5.getCode());
        }
        catch(Exception e){
            e.printStackTrace();
        }

        //kill the leader, print the leaders and send more requests
        System.out.println("killing the leader");
        servers.get(5).shutdown();
        servers.remove(servers.get(5));



        try {
            Thread.sleep(70000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());

            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            client.sendCompileAndRunRequest(validClass6);

            Client.Response r6 = client.getResponse();
            System.out.println("This is the response to be returned: " + r6.getBody());
            System.out.println("This is the status code to be returned: " + r6.getCode());

            client.sendCompileAndRunRequest(validClass7);

            Client.Response r7 = client.getResponse();
            System.out.println("This is the response to be returned: " + r7.getBody());
            System.out.println("This is the status code to be returned: " + r7.getCode());


            client.sendCompileAndRunRequest(validClass8);

            Client.Response r8 = client.getResponse();
            System.out.println("This is the response to be returned: " + r8.getBody());
            System.out.println("This is the status code to be returned: " + r8.getCode());
        }catch(Exception e){
            e.printStackTrace();
        }

        //step 5: stop servers
//        stopServers();
        for (ZooKeeperPeerServer server : servers) {
            server.shutdown();
        }
        gatewayServer.stop();

    }

}