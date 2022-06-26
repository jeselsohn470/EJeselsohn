package edu.yu.cs.com3800;

import edu.yu.cs.com3800.stage5.ZooKeeperPeerServerImpl;

import javax.management.Notification;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ZooKeeperLeaderElection implements LoggingServer{
    /**
     * time to wait once we believe we've reached the end of leader election.
     */
    private final static int finalizeWait = 3000;
    private ZooKeeperPeerServerImpl myPeerServer;
    private LinkedBlockingQueue<Message> incomingMessages;
    private ConcurrentHashMap<Long, ElectionNotification> votes = new ConcurrentHashMap<>();

    /**
     * Upper bound on the amount of time between two consecutive notification checks.
     * This impacts the amount of time to get the system up again after long partitions. Currently 60 seconds.
     */
    private final static int maxNotificationInterval = 60000;
    private long proposedLeader;
    private long proposedEpoch;
    private Logger logger;
    private ZooKeeperPeerServer.ServerState state;
    private Vote v = null;

    public ZooKeeperLeaderElection(ZooKeeperPeerServerImpl server, LinkedBlockingQueue<Message> incomingMessages) {
        this.incomingMessages = incomingMessages;
        this.myPeerServer = server;
        this.proposedLeader = this.myPeerServer.getServerId();
        this.proposedEpoch = this.myPeerServer.getPeerEpoch();
        this.votes.put(this.myPeerServer.getServerId(), new ElectionNotification(this.proposedLeader, this.myPeerServer.getPeerState(), this.myPeerServer.getServerId(), this.proposedEpoch));
        try {
            this.logger = initializeLogging(ZooKeeperLeaderElection.class.getCanonicalName() + "on-port: " + this.myPeerServer.getUdpPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.state = this.myPeerServer.getPeerState();

    }

    public static ElectionNotification getNotificationFromMessage(Message received) {

        ByteBuffer msgBytes = ByteBuffer.allocate(26);
        msgBytes.put(received.getMessageContents());
        msgBytes.flip();
        long leader = msgBytes.getLong();
        char stateChar = msgBytes.getChar();
        long senderID = msgBytes.getLong();
        long peerEpoch = msgBytes.getLong();
        return new ElectionNotification(leader, ZooKeeperPeerServer.ServerState.getServerState(stateChar), senderID, peerEpoch);
    }

    public static byte[] buildMsgContent(ElectionNotification notification) {

        ByteBuffer bytes = ByteBuffer.allocate(26);
        bytes.putLong(notification.getProposedLeaderID());
        char c = getChar(notification.getState());
        bytes.putChar(c);
        bytes.putLong(notification.getSenderID());
        bytes.putLong(notification.getPeerEpoch());
        return bytes.array();
    }

    public static char getChar(ZooKeeperPeerServer.ServerState state) {
        switch (state) {
            case LOOKING:
                return 'O';
            case LEADING:
                return 'E';
            case FOLLOWING:
                return 'F';
            case OBSERVER:
                return 'B';
        }
        return 'z';
    }


    private synchronized Vote getCurrentVote() {
        return new Vote(this.proposedLeader, this.proposedEpoch);
    }


    private void sendNotifications(){
        ElectionNotification elc = new ElectionNotification(this.proposedLeader, this.myPeerServer.getPeerState(), this.myPeerServer.getServerId(), this.proposedEpoch);
        this.myPeerServer.sendBroadcast(Message.MessageType.ELECTION, buildMsgContent(elc));
    }


    public synchronized Vote lookForLeader() {
        //send initial notifications to other peers to get things started
        this.logger.fine("looking for leader");
        int notTimeout = finalizeWait * 2;
        sendNotifications();
        Vote vote = null;
        try {

            //Loop, exchanging notifications with other servers until we find a leader
            while (this.myPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.LOOKING ||this.myPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.OBSERVER) {
                //Remove next notification from queue, timing out after 2 times the termination time
                Message message = this.incomingMessages.poll(notTimeout, TimeUnit.MILLISECONDS);
                if (message == null) {
                    this.logger.fine("message is null");
                    int timeTimeout = notTimeout * 2;
                    notTimeout = Math.min(timeTimeout, maxNotificationInterval);
                    sendNotifications();
                }
                else {
                    ElectionNotification electionNotification = getNotificationFromMessage(message);
                    ZooKeeperPeerServer.ServerState state = electionNotification.getState();
                    if (this.myPeerServer.isPeerDead(electionNotification.getProposedLeaderID())){
                        this.logger.fine(electionNotification.getProposedLeaderID() + " is dead");

                        continue;
                    }
                    //if no notifications received..
                    //..resend notifications to prompt a reply from others..
                    //.and implement exponential back-off when notifications not received..
                    //if/when we get a message and it's from a valid server and for a valid server..
                    //switch on the state of the sender:
                    this.logger.fine("This state is" + this.myPeerServer.getPeerState());
                    this.logger.fine(  " THe message is: " + electionNotification.getProposedLeaderID());
                    switch (state) {
                        case OBSERVER:
                            continue;
                        case LOOKING: //if the sender is also looking
                            vote = new Vote(electionNotification.getProposedLeaderID(), electionNotification.getPeerEpoch());
                            this.votes.put(electionNotification.getSenderID(), electionNotification);
                            //if the received message has a vote for a leader which supersedes mine, change my vote and tell all my peers what my new vote is.
                            this.logger.fine("my current vote is " + getCurrentVote().getProposedLeaderID() + " and i am checking if it is good");

                            if ((supersedesCurrentVote(electionNotification.getProposedLeaderID(), electionNotification.getPeerEpoch()) || (this.state == ZooKeeperPeerServer.ServerState.OBSERVER))) {
                                this.logger.fine("Old leader is " + this.proposedLeader + " and new one is "+ vote.getProposedLeaderID());
                                this.proposedEpoch = vote.getPeerEpoch();
                                this.proposedLeader = vote.getProposedLeaderID();
                                if (this.state != ZooKeeperPeerServer.ServerState.OBSERVER) {
                                    this.myPeerServer.setPeerState(ZooKeeperPeerServer.ServerState.LOOKING);
                                }
                                this.votes.put(myPeerServer.getServerId(), electionNotification);
                                sendNotifications();
                            }
                            //keep track of the votes I received and who I received them from.
                            ////if I have enough votes to declare my currently proposed leader as the leader:
                            //first check if there are any new votes for a higher ranked possible leader before I declare a leader. If so, continue in my election loop
                            //If not, set my own state to either LEADING (if I won the election) or FOLLOWING (if someone lese won the election) and exit the election
                            this.logger.fine("checking if " + getCurrentVote().getProposedLeaderID() + " has enough votes");
                            this.logger.fine(this.votes.toString());
                            if (haveEnoughVotes(this.votes, getCurrentVote())) {
                                this.logger.fine( getCurrentVote().getProposedLeaderID() + " has enough votes");

                                Message message1;
                                while ((message1 = this.incomingMessages.poll(finalizeWait, TimeUnit.MILLISECONDS)) != null) {
                                    this.logger.fine("This is an election message");
                                    ElectionNotification en = getNotificationFromMessage(message1);
                                    if (this.myPeerServer.isPeerDead(en.getProposedLeaderID())){
                                        this.logger.fine(en.getProposedLeaderID() + " is dead");
                                        continue;
                                    }

                                    this.logger.fine("checking to see if " + en.getProposedLeaderID() + " supersedes " + this.proposedLeader + " and the state of " + en.getProposedLeaderID() + " is " + en.getState()) ;
                                    if (en.getState() == ZooKeeperPeerServer.ServerState.LEADING || en.getState() == ZooKeeperPeerServer.ServerState.FOLLOWING){
                                        logger.fine("A leader was already established and the leader has the ID " + electionNotification.getProposedLeaderID());
                                        this.v = acceptElectionWinner(en);
                                        return this.v;
                                    }
                                    if (supersedesCurrentVote(en.getProposedLeaderID(), en.getPeerEpoch()) && en.getState() != ZooKeeperPeerServer.ServerState.OBSERVER && en.getState() == ZooKeeperPeerServer.ServerState.LOOKING) {
                                        this.logger.fine( "there is a higher vote than " + getCurrentVote().getProposedLeaderID()+ " and it is " + en.getProposedLeaderID() );
                                        this.incomingMessages.offer(message1);
                                        break;
                                    }
                                }
                                if (message1 == null){
                                    this.logger.fine("There are no more messages");
                                    ElectionNotification e = new ElectionNotification(this.proposedLeader, electionNotification.getState(), this.myPeerServer.getServerId(), this.proposedEpoch);
                                    this.v = acceptElectionWinner(e);
                                    return this.v;
                                }
                            }
                            else {
                                if (electionNotification.getState() == ZooKeeperPeerServer.ServerState.LEADING || electionNotification.getState() == ZooKeeperPeerServer.ServerState.FOLLOWING){
                                    logger.fine("A leader was already established and the leader has the id " + electionNotification.getProposedLeaderID());
                                    this.v = acceptElectionWinner(electionNotification);
                                    return this.v;
                                }
                                this.logger.fine(getCurrentVote().getProposedLeaderID() + " does not have enough votes");
                            }

                            break;
                        case FOLLOWING:
                        case LEADING: //if the sender is following a leader already or thinks it is the leader
                            //IF: see if the sender's vote allows me to reach a conclusion based on the election epoch that I'm in, i.e. it gives the majority to the vote of the FOLLOWING or LEADING peer whose vote I just received.
                            //if so, accept the election winner.
                            //As, once someone declares a winner, we are done. We are not worried about / accounting for misbehaving peers.
                            vote = new Vote(electionNotification.getProposedLeaderID(), electionNotification.getPeerEpoch());
                            this.votes.put(electionNotification.getSenderID(), electionNotification);
                            this.logger.fine(this.myPeerServer.getServerId() + " has a case " + this.myPeerServer.getPeerState() + " with leader being " + electionNotification.getProposedLeaderID()+ " and map is" + this.votes.toString());
                            if (haveEnoughVotes(this.votes, vote)  &&  vote.getPeerEpoch() >= this.proposedEpoch) {
                                this.v = acceptElectionWinner(electionNotification);
                                sendNotifications(); // send the notification
                            }
                            break;
                    }
                    if (this.v != null){
                        return this.v;
                    }
                }
            }
        }
        catch (InterruptedException e){
            return null;
        }

        return getCurrentVote();

    }

    private Vote acceptElectionWinner (ElectionNotification n) {
        //set my state to either LEADING or FOLLOWING
        //clear out the incoming queue before returning
        this.proposedLeader = n.getProposedLeaderID();
        this.proposedEpoch = n.getPeerEpoch();
        if(this.proposedLeader == myPeerServer.getServerId()){
            this.myPeerServer.setPeerState(ZooKeeperPeerServer.ServerState.LEADING);
            this.logger.fine(this.myPeerServer.getServerId() + ": switching from LOOKING to LEADING");
            System.out.println(this.myPeerServer.getServerId() + ": switching from LOOKING to LEADING\n");
        }
        else if (this.myPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.LOOKING){
            this.myPeerServer.setPeerState(ZooKeeperPeerServer.ServerState.FOLLOWING);
            this.logger.fine(this.myPeerServer.getServerId() + ": switching from LOOKING to FOLLOWING");
            System.out.println(this.myPeerServer.getServerId() + ": switching from LOOKING to FOLLOWING\n");
        }

        Vote vote = this.getCurrentVote();
        try {
            this.myPeerServer.setCurrentLeader(vote);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.incomingMessages.clear();
        return vote;

    }

    /*
     * We return true if one of the following three cases hold:
     * 1- New epoch is higher
     * 2- New epoch is the same as current epoch, but server id is higher.
     */
    protected boolean supersedesCurrentVote (long newId, long newEpoch){
        if (newEpoch > this.proposedEpoch){
            logger.fine("The epoch of ID " + newId + " is higher than the epoch of ID" + this.proposedLeader);
        }
        return (newEpoch > this.proposedEpoch) || ((newEpoch == this.proposedEpoch) && (newId > this.proposedLeader));
    }
    /**
     * Termination predicate. Given a set of votes, determines if have sufficient support for the proposal to declare the end of the election round.
     * Who voted for who isn't relevant, we only care that each server has one current vote
     */
    protected boolean haveEnoughVotes(Map<Long,ElectionNotification> votes, Vote proposal)
    {

        //is the number of votes for the proposal > the size of my peer server’s quorum?
        int c = 0; //count
        for (ElectionNotification e : votes.values()) {
            if (e.getState() == ZooKeeperPeerServer.ServerState.OBSERVER || proposal.getPeerEpoch() < this.proposedEpoch){
                continue;
            }
            if (e.getProposedLeaderID() == proposal.getProposedLeaderID() && e.getPeerEpoch() == proposal.getPeerEpoch()) {
                c++;
            }
        }
        return c >= this.myPeerServer.getQuorumSize();
    }

}