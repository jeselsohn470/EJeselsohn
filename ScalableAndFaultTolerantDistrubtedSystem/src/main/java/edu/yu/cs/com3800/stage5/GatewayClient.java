package edu.yu.cs.com3800.stage5;

import java.io.IOException;
import java.net.MalformedURLException;

public class GatewayClient {
    public static void main(String[] args) throws IOException {
        ClientImpl client = new ClientImpl("localhost", Integer.parseInt(args[0]));
        client.getElection();
    }
}
