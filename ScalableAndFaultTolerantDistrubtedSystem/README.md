Built a scalable and fault tolerant Distributed System in Java. 
Built a cluster of nodes which use the Master-Worker architecture. The server that wins the ZooKeeper Leader election functions as the master, and the other servers function as workers. The leader/master is the only server that:
- accepts requests from clients
- sends replies to clients
- assigns client requests to worker nodes that can do the work necessary to fulfill the requests.

Built a Gateway, which provides the public endpoint and API of my cluster and serves as the middleman between all clients and whichever node is currently the leader. 

Used an HTTP connection betweent the Client and Gateway, TCP connection between the Gateway and leader/master and a TCP connection between the leader/master and worker nodes. I also used a UDP connection for all the messages between servers that have to do with leader connection. 
Implemented a fault tolerant system using Gossip-Style heartbeats to detect node failures. If the leader node fails, then a new leader election is triggered and ensured no client requests were lost. 

