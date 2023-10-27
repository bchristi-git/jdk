import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * This test creates (and calls a method on) many Owner objects (that *do not*
 * protect against premature cleanup), each on a separate thread. The test
 * detects if/when the objects are cleaned up prematurely.
 */
public class PrematureCleanupTest {
    /* The test will run in one of the following modes */
    public static enum TestMode {
        COUNT("Test will create Owners on separate threads and count how many are cleaned up prematurely"),
        FORCE("Test will force all Owners to be cleaned up prematurely"),
        FORBID("Any prematurely cleaned Owner causes the test to fail. Test passes if it reaches the timeout duration with no prematurely cleaned Owners. Max number of owners is " + MAX_FORBID_OWNERS + ".");

        private final String description;
        
        TestMode(String desc) {
            this.description = desc;
        }
        
    }
    
    /* Mode for the test run; configurable via CLI */
    private static TestMode mode = TestMode.COUNT;    
    
    /* Test will timout after this amount of time; configurable via CLI */
    private static long TIMEOUT_MS = 30_000;

    /* Test will create this many Owner objects+threads; configurable via CLI */
    private static int NUM_OWNERS = 1000;

    /* The test will create no more than this many Owner object+threads in FORBID mode */
    private static final int MAX_FORBID_OWNERS = 500;

    private static boolean forbiddingPremature() {
        return mode == TestMode.FORBID;
    }
    private static boolean forcingPremature() {
        return mode == TestMode.FORCE || mode == TestMode.FORBID;
    }
    
    /* Duration between update messages when waiting for test to finish */
    private static final long MESSAGE_MS = 2000; 

    /* Values for measuring and reporting test activity */
    private static final AtomicInteger numPremature = new AtomicInteger(0);    
    private static final AtomicInteger doWorkFinished = new AtomicInteger(0);
    private static final AtomicInteger cleanersRun = new AtomicInteger(0);
    private static final AtomicBoolean alreadyFailed = new AtomicBoolean(false);
 
    private static final Cleaner SHARED_CLEANER = Cleaner.create();

    private static void printHelp() {
        System.out.println("""
            Arguments:
            "###"  - Number of Owner objects to create (e.g. "1000", the default for non-FORBID modes)

            "###s" - Timeout to wait, in seconds (e.g. "30s", the default)

            Test modes (choose one):);""");

        for(TestMode mode : TestMode.values()) {
            System.out.println(mode.name() + ": " + mode.description);            
        }
        System.out.println("");
        System.out.println("""                           
            The default set of arguments are:
            1000 30s COUNT
            """);
    }

    // Context for the Owner
    private static class Context implements Runnable {
        // The "resource"; volatile so as to see the cleaned up value on the test thread
        volatile long nativePointer;

        private Context() {
            nativePointer = 0x12345678L;
        }

        @Override
        public void run() {
            // "cleanup" the resource
            nativePointer = 0L;
            cleanersRun.getAndIncrement();
        }
    }

    /**
     * Typical Owner class (responsible for managing a "resource"). The doWork()
     * method is specially-crafted to cause and detect premature cleanup.
     */
    private static class Owner {    
        private final Context context;
        private Owner() {
            context = new Context();
            SHARED_CLEANER.register(this, context);
        }

        public void doWork() {
            // Add 'try' to make FORBID mode pass *without* ARRR
            // try {
            
            // Access the context/resource via a local, so Owner can become unreachable
            Context ctx = this.context;

            // Give 'this' a chance to become unreachable
            wasteTimeGCandPark();

            if(forcingPremature()) {
                // Wait for "resource" to be cleaned up
                while(ctx.nativePointer != 0) {
                    try {
                        Thread.sleep(1);
                    } catch(InterruptedException ie) {
                        // If not timed out, print stack trace?
                        break;
                    }
                }
            }

            // Use the resource again
            if (ctx.nativePointer == 0L) {
                numPremature.incrementAndGet();
                
                if (forbiddingPremature()) {                    
                    // Only one thread needs to report failure
                    if (!alreadyFailed.getAndSet(true)) {
                        throw new RuntimeException("TEST FAILED: Owner was cleaned up prematurely");
                    }
                }
            }
            
            doWorkFinished.getAndIncrement();

            // Add this 'catch' to make FORBID mode pass *without* ARRR
            // } finally {
            //     java.lang.ref.Reference.reachabilityFence(this);
            // }
        }
    }
   
    /* Create a (non-strongly-held) Owner object and call doWork() on it */
    private static class DoWorkRunnable implements Runnable {
        @Override
        public void run() {
            try {
                new Owner().doWork();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        
    }
    
    public static void main(String[] args) {
        parseArgs(args);
        runTest();
    }
    
    private static void parseArgs(String[] args) {
        // Parse arguments
        boolean badArg = false;
        for (String arg : args) {
            // Is it a timeout value?
            if (arg.endsWith("s")) {
                try {
                    String numVal = arg.substring(0, arg.length() - 1);
                    long tmp = Long.parseLong(numVal);
                    TIMEOUT_MS = tmp * 1000;
                    badArg = false;
                    continue;
                } catch (NumberFormatException e) {
                    badArg = true;
                }                
            }
            
            // Is it a number of Owners?
            try {
                int tmp = Integer.parseInt(arg);
                NUM_OWNERS = tmp;
                badArg = false;
                continue;
            } catch (NumberFormatException e) {
                badArg = true;
            }
            
            // Is it a test mode ?
            try {
                TestMode tmp = TestMode.valueOf(arg);
                mode = tmp;
                badArg = false;
                continue;
            } catch (IllegalArgumentException e) {
                badArg = true;
            }
            
            if (badArg) {
                System.out.println("Unknown argument: " + arg);
                printHelp();
                System.exit(1);
            }
        }

        if (mode == TestMode.FORBID && NUM_OWNERS > MAX_FORBID_OWNERS) {
            NUM_OWNERS = MAX_FORBID_OWNERS;
        }

        System.out.println("===========================");                        
        System.out.println("Test Configuration:");
        System.out.println("Test will create "+ NUM_OWNERS + " Owners");        
        
        if (forbiddingPremature() && !forcingPremature()) {
            throw new AssertionError("FORBID mode - must also force premature");
        }
        
        System.out.println(mode.name() + " mode: " + mode.description);
        System.out.println("Test will timeout after " + TIMEOUT_MS + "ms");
    }
    
    public static void runTest() {
        System.out.println("===========================");                
        System.out.println("Test running...");
        
        long maxTimeStart = System.currentTimeMillis();        
        
        // Create and start the threads
        Thread[] threads = new Thread[NUM_OWNERS];        
        long[] progressStart = { System.currentTimeMillis() };
        int numNulled = 0; // threads that have terminated and been nulled in the array
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new DoWorkRunnable());
            threads[i].start();            
            progressMessage(progressStart, i + " threads started");
        }
        
        System.out.println("All threads started; waiting for them to finish");
        
        // Wait for all threads to terminate
        while(numNulled < threads.length) {            
            // Read through array and null out threads that have terminated
            for (int i = 0; i < threads.length; i++) {
                if (threads[i] != null) {
                    if (threads[i].getState() != Thread.State.TERMINATED) {
                        try {
                            // wait a little for the thread to finish
                            threads[i].join(1);
                        } catch (InterruptedException e) {
                            // If not timed out, print stack trace?
                        }
                    }
                    if (threads[i].getState() == Thread.State.TERMINATED) {
                        threads[i] = null;
                        numNulled++;
                    }
                }
            }

            // Print periodic progress
            progressMessage(progressStart, numNulled + " threads done");

            // Let the gc run
            System.gc();
            try { Thread.sleep(1); } catch(InterruptedException ie) {}

            // Check for timeout or failure of FORBID mode
            boolean timeoutExceeded = System.currentTimeMillis() - maxTimeStart > TIMEOUT_MS;

            if (alreadyFailed.get() || timeoutExceeded) {
                // Shutdown the test
                if (timeoutExceeded) {
                    System.out.println("Maximum test time (" + TIMEOUT_MS + " ms) exceeded. Stopping test");
                }
                
                // Interrupt & null remaining threads
                for (int i = 0; i < threads.length; i++) {
                    if (threads[i] != null) {
                        threads[i].interrupt();
                        threads[i] = null;                        
                    }
                }
                
                // Did FORBID mode pass ?
                if (forbiddingPremature() && timeoutExceeded) {
                    if (numPremature.get() > 0) { // assert
                        throw new RuntimeException("FORBID mode: timeout exceeded, yet " + numPremature.get() + " premature Owners detected");
                    } else {
                        System.out.println("FORBID mode: test passed (timeout exceeded)");                            
                    }
                }
                break;
            }
        }
        if (!forbiddingPremature()) {
            report();
        }
    }    

    /*
     * @return whether message was printed & progressStart should be updated
    */
    private static boolean progressMessage(long[] start, String msg) {
        if (System.currentTimeMillis() - start[0] > MESSAGE_MS) {
            System.out.println(msg);
            start[0] = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }
    
    private static void report() {
        System.out.println("Owners collected prematurely: " + numPremature.get());
        int workFinished = doWorkFinished.get();
        if (workFinished != NUM_OWNERS) {
            System.out.println("*** only " + workFinished + "/" + NUM_OWNERS + " doWork() methods finished ***");
        }
        int cleanersFinished = cleanersRun.get();
        if (cleanersFinished != NUM_OWNERS) {
            System.out.println("*** only " + cleanersFinished + "/" + NUM_OWNERS + " cleaning actions finished ***");
        }
    }    
    
    private static void wasteTimeGCandPark() {
        for (int i = 0; i < 500_000; i++) {}
        System.gc();
        parkNanos(100_000L);
    }    
}
