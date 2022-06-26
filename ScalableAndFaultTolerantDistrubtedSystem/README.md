A couple notes on my code:

For starters, my computer is old and slow so every time I ran my code, it caused my computer to heat up and make lots of 
noise. 
That is why I had long sleep and wait times in my code. It also took a lot of playing around with the Gossip times and 
sleep times for it to work. Hence, the long wait time after something dies and the very short sleep time in between 
sending of Gossip messages. If the sleep time were any longer, then servers would think that servers that in reality did
not die, died.

Secondly, I have included two bash scripts. One that creates an output.log file and one that does not. I found that when
running the bash script that created the output.log file, it would sometimes fail. Not sure why. 
However, the one that never created the output.log file would never fail. Sometimes when running both the mvn test and 
the script one after another, it caused my computer to go into overdrive and the code did not work. 
That is why I commented out mvn test in my bash script and you can run them seperately or just uncomment it to run 
them together. Once again, I think this is because my computer is old and slow.

Lastly, because of all the reasons mentioned above and because the times on my computer are for sure very different 
than the times of your computer, I saved one of the output files for you to go through(stage5/output.log). 
In the output.log file, you can see the output from the two tests I ran 
(one where the follower dies and one where the leader dies) 
and the script (according to the steps you wanted).
You can see that in the script, the follower dies and then the leader dies and a new leader is elected and this new 
leader has an ID of 2. I also saved the log files from that test.
The log files for my MVN TEST are under the directory of com3800/stage5/logs 
and the log files for my SCRIPT are under the directory of com3800/stage5/src/main/java/logs
