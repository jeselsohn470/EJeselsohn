import java.util.HashMap;
import java.util.Map;

/**
 * This is a bounded cache that maintains only the most recently accessed IP Addresses
 * and their routes.  Only the least recently accessed route will be purged from the
 * cache when the cache exceeds capacity.  There are 2 closely coupled data structures:
 *   -  a Map keyed to IP Address, used for quick lookup
 *   -  a Queue of the N most recently accessed IP Addresses
 * All operations must be O(1).  A big hint how to make that happen is contained
 * in the type signature of the Map on line 38.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class RouteCache
{
    // instance variables - add others if you need them
    // do not change the names of any fields as the test code depends on them
    
    // Cache total capacity and current fill count.
    private final int capacity;
    private int nodeCount;
    
    // private class for nodes in a doubly-linked queue
    // used to keep most-recently-used data
    private class Node {
        private Node prev, next;
        private final IPAddress elem; 
        private final int route;

        Node(IPAddress elem, int route) {
            prev = next = null;
            this.elem = elem;
            this.route = route;
        }  
    }
    private Node head = null;
    private Node tail = null;
    private Map<IPAddress, Node> nodeMap; // the cache itself

    /**
     * Constructor for objects of class RouteCache
     */
    public RouteCache(int cacheCapacity)
    {
        this.capacity = cacheCapacity;
        this.nodeCount = 0;
        nodeMap = new HashMap<>();
    	// your code goes here
    }

    /**
     * Lookup the output port for an IP Address in the cache, adding it if not already there
     * 
     * @param  addr   a possibly cached IP Address
     * @return     the cached route for this address, or null if not found 
     */
    public Integer lookupRoute(IPAddress addr) {
        Node node = this.nodeMap.get(addr);
        if(node != null) {
            return node.route;
        }

        return null;
    }
     
    /**
     * Update the cache each time an element's route is looked up.
     * Make sure the element and its route is in the Map.
     * Enqueue the element at the tail of the queue if it is not already in the queue.  
     * Otherwise, move it from its current position to the tail of the queue.  If the queue
     * was already at capacity, remove and return the element at the head of the queue.
     * 
     * @param  elem  an element to be added to the queue, which may already be in the queue. 
     *               If it is, don't add it redundantly, but move it to the back of the queue
     * @return       the expired least recently used element, if any, or null
     */
    public IPAddress updateCache(IPAddress elem, int route)
    {

    	// your code goes here
        // your code goes here
        Node returnedNode = head;
        if (nodeMap.get(elem) == null){
            Node node = new Node(elem, route);
            nodeMap.put(elem, node);
            if (nodeCount == 0){
                head = node;
            }
            else{
                tail.prev = node;
                node.next = tail;
            }
            tail = node;

            nodeCount++;
            while (nodeCount>capacity){
                Node lastNode = head;

                if (head == tail){
                    tail = null;
                    head = null;
                    nodeMap.remove(lastNode.elem);
                    nodeCount--;
                    return lastNode.elem;
                }
                else{
                    head.prev.next = null;
                }
                head = head.prev;
                lastNode.prev = null;

                nodeCount--;
                nodeMap.remove(lastNode.elem);
                return returnedNode.elem;
            }
            return null;
        }
        Node updatedNode = nodeMap.get(elem);
        if (updatedNode == tail){
            return null;
        }
        if (updatedNode == head){
            Node temp = head;
            head = head.prev;
            temp.prev = null;
            head.next = null;
            temp.next = tail;
            tail.prev = temp;
            temp.prev = null;
            tail = temp;
            return null;
        }

        Node temp = updatedNode;
        temp.next.prev = temp.prev;
        temp.prev.next = temp.next;
        temp.prev = null;
        temp.next = null;
        Node tempTail = tail;
        tail = temp;
        tail.next = tempTail;
        tempTail.prev = tail;
        tail.prev = null;
        return null; //placeholder
    }

    /**
     * For testing and debugging, return the contents of the LRU queue in most-recent-first order,
     * as an array of IP Addresses in CIDR format. Return a zero length array if the cache is empty
     * 
     */
    String[] dumpQueue() {
        String[] stringArray = new String[nodeCount];

        if(this.tail != null) {
            int i = 0;
            Node tempTail = this.tail;
            while(tempTail != null) {
                stringArray[i] = tempTail.elem.toCIDR();
                i++;
                tempTail = tempTail.next;
            }
        }

        return stringArray;
    }

    
}
