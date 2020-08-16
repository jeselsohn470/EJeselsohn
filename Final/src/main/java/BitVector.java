import java.util.BitSet;
/**
 * Write a description of class BitVector here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class BitVector
{
    // instance variables - replace the example below with your own
    private final BitSet bits;
    private int length;

    /**
     * Constructors for objects of class BitVector
     */
    public BitVector(int length)
    {
        super();
        // initialise instance variables
        bits = new BitSet(length);
        this.length = length;
    }

    public BitVector()
    {
        this(32);
    }

    public int get(int index)
    {
        return bits.get(index) ? 1 : 0;
    }

    protected void set(int index, int value)
    {
        bits.set(index, (value != 0));
    }

    public int size()
    {
        return this.length;
    }

    public void setSize(int newSize)
    {
        if (newSize > bits.size()) throw new IllegalArgumentException("Bit vector too long");
        this.length = newSize;
    }

    @Override
    public String toString()
    {
        String res = "";
        for (int i = 0; i < this.length; i++) {
            res += this.get(i);
        }
        return res;

    }
    
    @Override
    public boolean equals(Object other)
    {
        if (! (other instanceof BitVector)) return false;
        return ((BitVector)other).bits.equals(this.bits);
    }
    
    @Override
    public int hashCode()
    {
        return this.bits.hashCode();
    }

}
