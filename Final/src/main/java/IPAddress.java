import java.util.Stack;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * Write a description of class IPAddress here.
 * 
 * @author Van Kelly
 * @version 0.1
 */
public class IPAddress extends BitVector
{

    /**
     * Constructor for objects of class IPAddress  Accepts a dotted decimal string in CIDR format and 
     * stores the equivalent BitVector internally
     * 
     * @param cidr  An IP Address or prefix of same in CIDR notation, e.g. "192.168.8.0/24"
     */
    public IPAddress(String cidr)
    {
        super();
        int length = 32; // default to an entire IP address
        String [] parts = cidr.split("/");
        if (parts.length < 1 | parts.length > 2) {
            throw new IllegalArgumentException("At most one / in a CIDR string.");
        }
        if (parts.length == 2) {
            length = Integer.parseInt(parts[1]);
            if (length < 1 || length > 32) {
                throw new IllegalArgumentException("Illegal length for a CIDR prefix.");
            }
        }
        parts = parts[0].split("\\.");
        this.setSize(length);
        int k = 32; 
        if (parts.length != 4) {
            throw new IllegalArgumentException("Need four numbers in an IP Address");
        }
        for (int i = 3; i > -1; --i) {
            int octet = Integer.parseInt(parts[i]);
            for (int j = 0; j < 8; j++) {
                if (--k < length) {
                    this.set(k, octet % 2);
                }
                octet /= 2;
            }
        }
        //return bits;
    }

    /**
     * For debugging and logging only, return the stored IP Address or prefix in CIDR notation.
     * 
     */
    public String toCIDR() {
        int [] octets = {0, 0, 0, 0};
        for (int i = 0 ; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                octets[i] = (octets[i] << 1) + this.get(i*8 + j);
            }
        }
        String res = "" + octets[0];
        for (int i = 1; i < 4; i++) {
            res += "." + octets[i];
        }
        if (this.size() < 32) {
            res += "/" + this.size();
        }
        return res;              

    }
}
