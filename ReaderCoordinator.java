import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.Random;


public class ReaderCoordinator {

    // initializing variables and Semaphores / setup for problem
    private static Semaphore readerSpots ;       // only a certain amt of readers at a time
    private static Semaphore coordTurn;       // SIGNAL: readers done and writers can begin writing
    private static Semaphore mutex;             //
    private static Semaphore printMutex = new Semaphore(1,true); /* NEEDED to print mutex without
                                                                                    messing up the aquiring and releasing*/

    private static int completedReaders = 0; // current # of readers finished in the round
    private static int totalReadersDone = 0;    // # of overall readers finished in ALL rounds

    private static final Random random = new Random(); //allowing for later usage of random

    private static int R,W,N;       // variables for user inputted values


    // Reader Thread!
    static class Reader extends Thread {
        private final int id;

        // id constructor
        Reader(int id) {
            super ("R" + id);       // allows for the threads to be named R then id
            this.id = id;
        }

        /* acts as the time taken to actually read the data in the
             shared space */
        private void readCycle() throws InterruptedException {
            Thread.sleep((3 + random.nextInt(4)) * 10);
        }

        //formatting of the shared space logs and allocation to console
        private void mutexLog(String msg) throws InterruptedException {
            printMutex.acquire();
            System.out.println(msg);
            printMutex.release();
        }
        /* thread waits until it is allowed to read
            then logs that it has began .... if interrupted... it
            logs that and program handles as defined */

        @Override
        public void run() {
            try {
                /* thread begins to wait for a reader slot
                    once slot is available then it will acquire the resource
                    and begin reading for a time */
                readerSpots.acquire();

                mutexLog("R" + id + " started reading.");
                readCycle();
                mutex.acquire();

                // update the total amount of readers done
                completedReaders++;
                totalReadersDone++;

                mutexLog("-R" + id + " finished reading. Total Reads: " + completedReaders);

                /* If all N readers are finished... round is done
                signal the writer */

                if(completedReaders >= N || totalReadersDone == R) {
                    completedReaders = 0;   // resets the round count to 0
                    coordTurn.release();    // signal writer can begin
                }

                mutex.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    // Coordinator / Writer Thread!
    static class Writer extends Thread {
        private final int id;

        Writer (int id){
            super("W" + id);    // allows for the threads to be called W then id
            this.id = id;
        }
        /* acts as the time taken to actually write the data in the
             shared space */
        private void cycleWrite() throws InterruptedException{
            Thread.sleep((3+random.nextInt(4))*10);
        }
        //formatting and shared space allocation
        private void mutexLog(String msg) throws InterruptedException {
            printMutex.acquire();
            System.out.println(msg);
            printMutex.release();
        }

        /* thread waits until it is allowed to write (no readers)
            then logs that it has began .... if interrupted... it
            logs that and program handles as defined */

        @Override
        public void run(){
            try{
                coordTurn.acquire();    /* waits until there is a signal to start
                                            will acquire the resource after */
                mutexLog("--" + getName() + " started writing");
                cycleWrite();
                mutexLog("--" + getName() + " finished writing");

                // Reopen user inputted # of reader spots
                for (int i=0; i< N; i++){
                    readerSpots.release();
                }

            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }

    }

     public static void main () throws InterruptedException{
        System.out.println("Welcome to the Readers-Writers' problem!");
        // function throws except to handle waiting, sleeping, or occupied threads
        Scanner scanner = new Scanner(System.in);

        // user inputs
        System.out.print ("Enter Readers count between 1 and 10,000. ");
        R = scanner.nextInt();
        if (R < 1 || R > 10000)
            throw new InterruptedException("Invaild number");

        System.out.print ("Enter Writers count between 1 and 10,000. ");
        W = scanner.nextInt();
        if (W < 1 || W > 10000)
            throw new InterruptedException("Invaild number");

        System.out.print ("Enter the Max simultaneous Reader count between 1 and 45. ");
        N = scanner.nextInt();
        if (N < 1 || N > 45)
            throw new InterruptedException("Invaild number");

        scanner.close();//no more inputs
        long starttime = System.nanoTime();

        readerSpots = new Semaphore(N,true);    // max # of simultaneous readers
        coordTurn = new Semaphore(0,true); // allows for the pattern N readers then 1 writer
        mutex = new Semaphore(1,true);

        //2 arrays of threads (readers and writers) for easy handling
        Thread[] readers = new Thread[R];
        Thread[] writers = new Thread[W];

        //creates a new reader thread for the number inputted by user
        for (int i = 0; i < R; i++){
            readers[i] = new Reader(i);
        }

        for (int i = 0; i < W; i++){
            writers[i] = new Writer(i);
        }
// Create the threads
            // reader threads
        for (int rCount = 0; rCount < R; rCount++){
            readers[rCount].start();
        }
            // writer / coordinator threads
        for (int wCount = 0; wCount < W; wCount++){
            writers[wCount].start();
            }

        // Waits until all reader and writer threads are done
        int handleReaders = Math.min(R,(W+1) *N); /* VERY IMPORTANT LINES... this logic handles the deadlock of the readers
                                                        allows for the join() to execute after last writer and set of readers.
                                                        remaining readers never access the resource! */
        int handleWriters = Math.min(W,(R+N-1)/N);

        try {
            for (int i = 0; i < handleReaders; i++) {
                readers[i].join();
            }
            for (int i = 0; i < handleWriters; i++) {
                writers[i].join();
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();;
        }


        System.out.println("No threads ready or runnable, and no pending interrupts.");
        System.out.println("Assuming the program completed.");
        System.out.println("Machine halting :)!");

        long end_time = System.nanoTime();
        System.out.printf("Runtime in milliseconds = ");
        System.out.println((end_time - starttime) / 1000000.0);

        System.exit(0); //finalize the join threads

    }
}
