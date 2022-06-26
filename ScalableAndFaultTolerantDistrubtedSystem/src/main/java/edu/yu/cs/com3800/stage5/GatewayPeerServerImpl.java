package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.Vote;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class GatewayPeerServerImpl extends ZooKeeperPeerServerImpl {

    private List<Integer> observerPorts;
    private Map<Long, InetSocketAddress> peerIDtoAddress;

    public GatewayPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long, InetSocketAddress> peerIDtoAddress, List<Integer> observerPorts) {
        super(myPort, peerEpoch, id, peerIDtoAddress, observerPorts);
        //have to set it to observer because by default it is set to looking
        setPeerState(ZooKeeperPeerServer.ServerState.OBSERVER);
        this.observerPorts = observerPorts;
        this.peerIDtoAddress = peerIDtoAddress;
    }


    @Override
    public ZooKeeperPeerServer.ServerState getPeerState() {
        return ZooKeeperPeerServer.ServerState.OBSERVER;
    }

    public InetSocketAddress getLeader(){
        //need to check if the leader is still alive

        return getPeerByID(getCurrentLeader().getProposedLeaderID());
    }

    public boolean isLeaderDead(){
        if (getGossipThread().checkLeaderFailed(getCurrentLeader().getProposedLeaderID())){
            return true;
        }
        return false;
    }

    public boolean leaderALive(){
        return getCurrentLeader().getProposedLeaderID() != getServerId();
    }

    @Override
    public void shutdown() {

        super.shutdown();
    }



}


