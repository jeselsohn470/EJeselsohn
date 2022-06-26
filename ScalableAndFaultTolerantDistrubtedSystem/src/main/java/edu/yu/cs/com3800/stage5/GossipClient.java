package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.stage5.Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GossipClient {
    public HttpClient client;
    public HttpResponse<String> responseNameReturn = null;
    String hostName;
    int hostPort;
    private HttpResponse<String> completableFuture;

    public GossipClient(String hostName, int hostPort) throws MalformedURLException {
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

    public void getRequest(String src) throws IOException {
        String uri = "http://" + hostName + ":" + hostPort + "/gossipMessages";
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                //Where we are sending the data
                .uri(URI.create(uri))
                //Content Type
                .setHeader("Content-Type", "text/x-java-source")
                .build();
        try
        {
            completableFuture = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    public Client.Response getResponse()  {

        if(responseNameReturn == null)
            return null;
        return new Client.Response(responseNameReturn.statusCode(), responseNameReturn.body());
    }

}