
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.*;

/**
 * The test class TestRouter.
 *
 * @author  (your name)
 * @version (a version number or a date)
 */
public class TestRouter
{

    //private TrieST<Integer> router;
    private IPRouter router;
    private BitVectorTrie<Integer> bitVectorTrie;

    /**
     * Default constructor for test class TestRouter
     */
    public TestRouter()
    {

    }

    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    @Before
    public void setUp()
    {
        bitVectorTrie = new BitVectorTrie<>();
        this.router = new IPRouter(8,4);
        try {
            router.loadRoutes("routes2.txt");
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("Bad routes file name. Tests aborted");
        }
    }


    /**
     * test the bitVectorTrie Sunny day cases
     */
    @Test
    public void bitVectorTrieTest()
    {
        IPAddress address = new IPAddress("85.85.85.85");
        IPAddress address1 = new IPAddress("85.85.85.86");
        bitVectorTrie.put(address,5);
        bitVectorTrie.put(address1,6);
        Integer in = bitVectorTrie.get(address);
        assertEquals((Integer)5,in);

        Integer in1 = bitVectorTrie.get(address1);
        assertEquals((Integer)6,in1);

        bitVectorTrie.delete(address);
        assertNull(bitVectorTrie.get(address));

    }

    /**
     * border cases where an illegal argument should be thrown
     */
    @Test (expected = IllegalArgumentException.class)
    public void replace(){
        IPAddress address = new IPAddress("85.85.85.85");
        IPAddress address1 = new IPAddress("85.85.85.85");
        bitVectorTrie.put(address,5);
        bitVectorTrie.put(address1,6);
    }
    @Test (expected = IllegalArgumentException.class)
    public void delete(){
        IPAddress address = new IPAddress("85.85.85.85");
        router.addRule("85.85.85.85", -1);
    }
    @Test (expected = IllegalArgumentException.class)
    public void delete1(){
        IPAddress address = new IPAddress("85.85.85.85");
        router.addRule("85.85.85.85", 8);
    }

    /**
     * thrown an exception because port<0 or port is greater than nport-1
     */
    @Test (expected = IllegalArgumentException.class)
    public void addRuleExcpetion(){
        IPAddress address = new IPAddress("85.85.85.85");
        IPAddress address1 = new IPAddress("85.85.85.85");
        bitVectorTrie.delete(address);
    }
    /**
     * Handle an unroutable address
     */
    @Test
    public void testBadRoute()
    {
        IPAddress address = new IPAddress("73.73.0.1");
        assertEquals(-1, this.router.getRoute(address));
    }

    /**
     * Handle an address that only matches one prefix
     */
    @Test
    public void port2Test()
    {
        IPAddress address = new IPAddress("85.2.0.1");
        int res = this.router.getRoute(address);
        assertEquals(2, res);
    }

    /**
     * Handle an address that only matches multiple prefixes. Only the longest one counts
     */
    @Test
    public void port1Test()
    {
        IPAddress address = new IPAddress("85.85.85.85");
        int res = this.router.getRoute(address);
        assertEquals(1, res);
    }

    /**
     * deleteroute test
     */
    @Test (expected = IllegalArgumentException.class)
    public void deleteRouteTestIAE()
    {
        IPAddress address = new IPAddress("85.85.85.85");
        IPAddress address2 = new IPAddress("85.85.0.0/15");
        int res = this.router.getRoute(address);
        assertEquals(1, res);
        this.router.deleteRule(address.toCIDR());
        assertEquals(1, this.router.getRoute(address));
        assertNull(bitVectorTrie.get(address));
    }
    @Test
    public void deleteRouteTest()
    {
        IPAddress address = new IPAddress("85.85.85.85");
        IPAddress address2 = new IPAddress("85.85.0.0/15");
        int res = this.router.getRoute(address);
        assertEquals(1, res);

        this.router.deleteRule(address2.toCIDR());
        assertEquals(1, this.router.getRoute(address));
        this.router.addRule(address2.toCIDR(), 7);
    }

    /**
     * test the getRoute method with lots of IP Addresses
     */

    @Test
    public void port3Test()
    {
        IPAddress address = new IPAddress("85.2.0.1");
        int res = this.router.getRoute(address);
        int res23 = this.router.getRoute(address);
        assertEquals(2, res);
        assertEquals(1, router.dumpCache().length);

        IPAddress address1 = new IPAddress("85.85.85.85");
        int res1 = this.router.getRoute(address1);
        assertEquals(1, res1);

        IPAddress address2 = new IPAddress("24.98.0.0");
        int res2 = this.router.getRoute(address2);
        assertEquals(6, res2);

        IPAddress address3 = new IPAddress("24.60.0.0");
        int res3 = this.router.getRoute(address3);
        assertEquals(5, res3);

        //makes the head go away
        IPAddress address4 = new IPAddress("24.128.0.0");
        int res4 = this.router.getRoute(address4);
        assertEquals(3, res4);

        IPAddress address5 = new IPAddress("73.73.0.1");

//test to see if it is in cache - address shouldnt be in caches
        assertTrue(this.router.isCached(address3));
        assertEquals(5, this.router.getRoute(address3));

        assertTrue(this.router.isCached(address2));
        assertEquals(6, this.router.getRoute(address2));

        assertTrue(this.router.isCached(address1));
        assertEquals(1, this.router.getRoute(address1));

        assertEquals(false, this.router.isCached(address));
        assertEquals(2, this.router.getRoute(address));
//address is now in chache
        assertTrue(this.router.isCached(address));
        assertEquals(false, this.router.isCached(address4));

        assertEquals(-1, this.router.getRoute(address5));
        assertTrue(this.router.isCached(address5));
        assertEquals(false, this.router.isCached(address3));

        assertEquals(4, router.dumpCache().length);
        assertEquals("24.98.0.0",router.dumpCache()[3]);
        assertEquals("85.85.85.85",router.dumpCache()[2]);
        assertEquals("85.2.0.1",router.dumpCache()[1]);
        assertEquals("73.73.0.1",router.dumpCache()[0]);
    }

    /**
     * testing 2 or more overlapping rules for one address
     */
    @Test
    public void test2ormore(){
        IPAddress address = new IPAddress("24.64.0.0/10");
        int res = this.router.getRoute(address);
        assertEquals(4, res);



        IPAddress address2 = new IPAddress("24.98.0.0/15");
        int res2 = this.router.getRoute(address2);
        assertEquals(6, res2);
        int res23 = this.router.getRoute(address);
        assertEquals(4, res23);
        assertEquals(2, router.dumpCache().length);
        assertEquals("24.98.0.0/15",router.dumpCache()[1]);
        assertEquals("24.64.0.0/10",router.dumpCache()[0]);
        this.router.deleteRule("24.64.0.0/10");
        assertEquals(6, this.router.getRoute(address2));
        this.router.addRule("24.64.0.0/10", 4);
        this.router.addRule("24.98.0.0/15", 6);

    }
    @Test
    public void test2ormore2(){
        IPAddress address = new IPAddress("24.64.0.0/10");
        int res = this.router.getRoute(address);
        assertEquals(4, res);
        this.bitVectorTrie.put(address,4);



        IPAddress address2 = new IPAddress("24.98.0.0/15");
        int res2 = this.router.getRoute(address2);
        assertEquals(6, res2);
        this.bitVectorTrie.put(address2,6);

       this.bitVectorTrie.delete(address2);
       assertEquals((Integer)4,bitVectorTrie.get(address));

    }

    @Test
    public void Trie3OverlapTest(){
        IPRouter ipr = new IPRouter(8, 2);
        ipr.addRule("24.64.0.0/10", 5);
        ipr.addRule("24.91.0.0/16", 6);
        IPAddress ip = new IPAddress("24.91.0.0/14");
        IPAddress ip1 = new IPAddress("24.91.23.198/20");
        assertEquals(5, ipr.getRoute(ip));
        assertEquals(6, ipr.getRoute(ip1));
        ipr.deleteRule("24.91.0.0/16");
        assertEquals(6, ipr.getRoute(ip1));
    }
    @Test
    public void CachedSize0BorderTest() {
        this.router = new IPRouter(8, 0);
        try {
            router.loadRoutes("routes2.txt");
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("Bad routes file name. Tests aborted");
        }

        IPAddress address = new IPAddress("85.2.0.1");
        int res = this.router.getRoute(address);
        assertEquals(2, res);
        assertEquals(0, router.dumpCache().length);
        assertFalse(this.router.isCached(address));
    }
    @Test (expected = IllegalArgumentException.class)
    public void IATest() throws FileNotFoundException {
        IPRouter iprouter = new IPRouter(6, 3);
        iprouter.deleteRule("24.128.0.0/1");

    }
    @Test (expected = IllegalArgumentException.class)
    public void IATest1() throws FileNotFoundException {
        IPRouter iprouter = new IPRouter(15, 2);
        iprouter.loadRoutes("routes2.txt");
        iprouter.deleteRule("24.128.0.0/29");

    }

    @Test (expected =  IllegalArgumentException.class)
    public void portOverSize(){
        router.addRule("24.98.0.0/15", 10);
    }
    @Test
    public void CheckRouteAfterDelete() {
        router.deleteRule("24.98.0.0/15");
        IPAddress ipAddress = new IPAddress("24.98.0.0/15");
        assertEquals(4, router.getRoute(ipAddress));
    }



    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    @After
    public void tearDown()
    {

    }
}
