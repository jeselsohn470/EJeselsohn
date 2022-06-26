
#!/bin/bash
#eitan jeselsohn and jonathan crane
#exec &> output.log

echo "running the bash script"
echo " "

echo "running my test code" | tee -a output.log
sleep 2
mvn test | tee -a output.log
echo"" | tee -a output.log


echo ""
sleep 2
echo "Now I am running the script" | tee -a output.log
echo ""
echo "compiling the src code"  | tee -a output.log
echo ""
javac src/main/java/edu/yu/cs/com3800/*.java src/main/java/edu/yu/cs/com3800/stage5/*.java -Xlint
cd src/main/java

echo "starting up the 7 servers and gateway" | tee -a output.log
echo ""

java edu/yu/cs/com3800/stage5/TestScript 15 &
script_fifteen=$!
#this sleep is to make sure  the gateway server has enough time to start up
sleep 5
java edu/yu/cs/com3800/stage5/TestScript 0 &
script_zero=$!
java edu/yu/cs/com3800/stage5/TestScript 1 &
script_one=$!
java edu/yu/cs/com3800/stage5/TestScript 2 &
script_two=$!
java edu/yu/cs/com3800/stage5/TestScript 3 &
script_three=$!
java edu/yu/cs/com3800/stage5/TestScript 4 &
script_four=$!

java edu/yu/cs/com3800/stage5/TestScript 5 &
script_five=$!
java edu/yu/cs/com3800/stage5/TestScript 6 &
script_six=$!


#this sleep is just to get the leader election started
sleep 10

echo ""
echo "entered step 3"  | tee -a output.log
#3
#will busy wait until the election is completed
#once completed, prints out the election
echo ""
java edu/yu/cs/com3800/stage5/GatewayClient 8080 | tee -a output.log


#4
echo ""
echo "entered step 4"
echo ""
java edu/yu/cs/com3800/stage5/ClientImpl 8080 0 | tee -a output.log
#Make a client class that sends 9 requests to the Gateway
#print out request before sending
#print response

#5
echo ""
echo "entered step 5" | tee -a output.log
kill -9 $script_five
echo ""
echo "Killing the follower server with the ID 5" | tee -a output.log
echo ""
#I needed to sleep this amount of time. Otherwise, not all the nodes would pick up on the dead one
#Probably needed this much time because my computer is old and slow
sleep 50
#print out all the servers, and 5 is not there
java edu/yu/cs/com3800/stage5/GatewayClient 8080 | tee -a output.log


#6
echo ""
echo "entered steps 6 and 7" | tee -a output.log
kill -9 $script_six
echo ""
echo "Leader is killed"  | tee -a output.log
echo ""
sleep 1
#send out 9 requests and don't wait for responses
java edu/yu/cs/com3800/stage5/ClientImpl 8080 1 | tee -a output.log



#7

#echo ""
#java edu/yu/cs/com3800/stage5/GatewayClient 8080


#busy wait on gateway leader eleciton
#get node id of leader-- uild another client request with a different header
#print out the 9 messages sent

#8
echo ""
echo "entered step 8" | tee -a output.log
#send one more request
echo ""
java edu/yu/cs/com3800/stage5/ClientImpl 8080 3 | tee -a output.log

#9
echo ""
echo "entered step 9" | tee -a output.log
echo ""
echo "The paths to the files containing the Gossip messages received by each node are in a directory called com3800/stage5/src/main/java/logs/GossipThreadMessages" | tee -a output.log

#10
echo ""
echo "entered step 10"  | tee -a output.log
echo " "
echo "killing the servers" | tee -a output.log
kill -9 $script_zero
kill -9 $script_one
kill -9 $script_two
kill -9 $script_three
kill -9 $script_four
kill -9 $script_fifteen

echo ""
echo "the log files for my MVN TEST are under the directory of com3800/stage5/logs" | tee -a output.log
echo ""
echo "the log files for my SCRIPT are under the directory of com3800/stage5/src/main/java/logs" | tee -a output.log


