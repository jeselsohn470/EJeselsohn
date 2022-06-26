package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.stage5.Client;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;

public class ClientImpl implements Client {
    public HttpClient client;
    String hostName;
    int hostPort;
    private Queue<CompletableFuture<HttpResponse<String>>> completableFuture = new ConcurrentLinkedDeque<>();

    public ClientImpl(String hostName, int hostPort) throws MalformedURLException {
        if(hostName == null || hostPort < 0){
            throw new IllegalArgumentException("The given hostname or hostPort are out of range");
        }
        this.hostName = hostName;
        this.hostPort = hostPort;
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .proxy(ProxySelector.of(new InetSocketAddress(hostName, hostPort)))
                .build();
    }


    @Override
    public void sendCompileAndRunRequest(String src) throws IOException {
        String uri = "http://" + hostName + ":" + hostPort + "/compileandrun";
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(src))
                //Where we are sending the data
                .uri(URI.create(uri))
                //Content Type
                .setHeader("Content-Type", "text/x-java-source")
                .build();
        completableFuture.offer(this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
    }

    public void getElection() throws IOException {
        String uri = "http://" + hostName + ":" + (hostPort) + "/compileandrun";
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                //Where we are sending the data
                .uri(URI.create(uri))
                //Content Type
                .setHeader("Content-Type", "hasLeader")
                .build();

        while(true){
            try
            {
                completableFuture.offer(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
                try {
                    if(!completableFuture.peek().get().body().equals("leader not elected")){
                        break;
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

        }
        //Print out the leader election
        try {
            System.out.println(completableFuture.poll().get().body());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //POLL GATeWAY to GET EVERYONE SERVERS, STATUS ID,
    }


    @Override
    public Response getResponse() {
        if (completableFuture == null || completableFuture.isEmpty())
            return null;
        CompletableFuture<HttpResponse<String>> httpResponse = completableFuture.poll();
        try {
            return new Response(httpResponse.get().statusCode(), httpResponse.get().body());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException, InterruptedException {


        //send response and wait for reply
        if(Integer.parseInt(args[1]) == 0)
        {
            Client client = null;
            client = new ClientImpl("localhost", Integer.parseInt(args[0]));

            System.out.println("\nThis is request # 1: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 1\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 1\";\n    }\n}\n");

            System.out.println("\nThis is the request # 2: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 2\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 2\";\n    }\n}\n");
//
            System.out.println("\nThis is request #3: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 3\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 3\";\n    }\n}\n");

            System.out.println("\nThis is request #4: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 4\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 4\";\n    }\n}\n");

            System.out.println("\nThis is request #5: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 5\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 5\";\n    }\n}\n");

            System.out.println("\nThis is request #6: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 6\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 6\";\n    }\n}\n");

            System.out.println("\nThis is request #7: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 7\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 7\";\n    }\n}\n");

            System.out.println("\nThis is request #8: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 8\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 8\";\n    }\n}\n");

            System.out.println("\nThis is request #9: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 9\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 9\";\n    }\n}\n");

            System.out.println();

            Response r1 =  client.getResponse();
            Response r2 = client.getResponse();
            Response r3 = client.getResponse();
            Response r4 = client.getResponse();
            Response r5 = client.getResponse();
            Response r6 = client.getResponse();
            Response r7 = client.getResponse();
            Response r8 = client.getResponse();
            Response r9 = client.getResponse();

            System.out.println("\nThis response #1 to be returned: " + r1.getBody());
            System.out.println("\nThis is the status code #1 to be returned: " + r1.getCode());
            System.out.println("\nThis response #2 to be returned: " + r2.getBody());
            System.out.println("\nThis is the status code #2 to be returned: " + r2.getCode());
            System.out.println("\nThis response #3 to be returned: " + r3.getBody());
            System.out.println("\nThis is the status code #3 to be returned: " + r3.getCode());
            System.out.println("\nThis response #4 to be returned: " + r4.getBody());
            System.out.println("\nThis is the status code #4 to be returned: " + r4.getCode());
            System.out.println("\nThis response #5 to be returned: " + r5.getBody());
            System.out.println("\nThis is the status code #5 to be returned: " + r5.getCode());
            System.out.println("\nThis response #6 to be returned: " + r6.getBody());
            System.out.println("\nThis is the status code #6 to be returned: " + r6.getCode());
            System.out.println("\nThis response #7 to be returned: " + r7.getBody());
            System.out.println("\nThis is the status code #7 to be returned: " + r7.getCode());
            System.out.println("\nThis response #8 to be returned: " + r8.getBody());
            System.out.println("\nThis is the status code #8 to be returned: " + r8.getCode());
            System.out.println("\nThis response #9 to be returned: " + r9.getBody());
            System.out.println("\nThis is the status code #9 to be returned: " + r9.getCode());
        }
        //then i just send the messages
        else if(Integer.parseInt(args[1]) == 1){
            ClientImpl client = null;
            client = new ClientImpl("localhost", Integer.parseInt(args[0]));
//            System.out.println("It is  getting here 0.0");
            System.out.println("\nThis is request #1: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 1\";\n    }\n}\n");

            System.out.println("\nThis is request #2: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 2\";\n    }\n}\n");

            System.out.println("\nThis is request #3: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 3\";\n    }\n}\n");

            System.out.println("\nThis is request #4: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 4\";\n    }\n}\n");

            System.out.println("\nThis is request #5: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 5\";\n    }\n}\n");

            System.out.println("\nThis is request #6: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 6\";\n    }\n}\n");

            System.out.println("\nThis is request #7: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 7\";\n    }\n}\n");

            System.out.println("\nThis is request #8: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 8\";\n    }\n}\n");

            System.out.println("\nThis is request #9: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 9\";\n    }\n}\n");


            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 1\";\n    }\n}\n");

            Response s1 = client.getResponse();


//            System.out.println("It is getting here 1.0");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 2\";\n    }\n}\n");
            Response s2 = client.getResponse();

            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 3\";\n    }\n}\n");
            Response s3 = client.getResponse();

            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 4\";\n    }\n}\n");
            Response s4 = client.getResponse();

            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 5\";\n    }\n}\n");
            Response s5 = client.getResponse();

            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 6\";\n    }\n}\n");
            Response s6 = client.getResponse();

            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 7\";\n    }\n}\n");
            Response s7 = client.getResponse();

            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 8\";\n    }\n}\n");
            Response s8 = client.getResponse();

            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 9\";\n    }\n}\n");
            Response s9 = client.getResponse();

            Thread.sleep(2000);
            System.out.println();
            client.getElection();


            System.out.println();

            System.out.println("\nThis response #1 to be returned: Hello world! 1" + s1.getBody());
            System.out.println("\nThis is the status code #1 to be returned: " + 200);
            System.out.println();
            System.out.println("\nThis response #2 to be returned: " + s2.getBody());
            System.out.println("\nThis is the status code #2 to be returned: " + s2.getCode());

            System.out.println();
            System.out.println("\nThis response #3 to be returned: " + s3.getBody());
            System.out.println("\nThis is the status code #3 to be returned: " + s3.getCode());

            System.out.println();
            System.out.println("\nThis response #4 to be returned: " + s4.getBody());
            System.out.println("\nThis is the status code #4 to be returned: " + s4.getCode());

            System.out.println();
            System.out.println("\nThis response #5 to be returned: " + s5.getBody());
            System.out.println("\nThis is the status code #5 to be returned: " + s5.getCode());

            System.out.println();
            System.out.println("\nThis response #6 to be returned: " + s6.getBody());
            System.out.println("\nThis is the status code #6 to be returned: " + s6.getCode());

            System.out.println();
            System.out.println("\nThis response #7 to be returned: " + s7.getBody());
            System.out.println("\nThis is the status code #7 to be returned: " + s7.getCode());

            System.out.println();
            System.out.println("\nThis response #8 to be returned: " + s8.getBody());
            System.out.println("\nThis is the status code #8 to be returned: " + s8.getCode());

            System.out.println();
            System.out.println("\nThis response #9 to be returned: " + s9.getBody());
            System.out.println("\nThis is the status code #9 to be returned: " + s9.getCode());
            System.out.println();


        }
        else{
            Client client = null;
            client = new ClientImpl("localhost", Integer.parseInt(args[0]));

            System.out.println("\nThis is request #1: \n");
            System.out.println("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 1\";\n    }\n}\n");
            client.sendCompileAndRunRequest("package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world! 1\";\n    }\n}\n");
            Response r = client.getResponse();
            System.out.println("\nThis is the response to be returned: " + r.getBody());
            System.out.println("\nThis is the status to be returned: " + r.getCode());


        }

    }

}

