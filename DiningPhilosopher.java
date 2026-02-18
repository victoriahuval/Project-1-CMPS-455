import java.util.concurrent.Semaphore;
import java.util.Random;
import java.util.Scanner;
public class DiningPhilosopher {
    //this is to track the chopsticks
    private static Semaphore [] chops;
    // -- arrival --
    //checks to see whether the philosophers have arrived or not.
    //will release in main once all philosophers have arrived.
    private static Semaphore arriveCount;
    //this counts the arrival of the philosopher (it will start at 0)
    private static Semaphore arrivalDone;
    // signaled in main when all philosophers are at the table

    // -- leaving --
    //philosophers are supposed to wait at the table until ALL philosophers have finished eating
    //they will all leave together :)
    private static Semaphore finishedCount; //counts for finished philosopher
    private static Semaphore releaseAll; //releases all of the philosophers from the table in main

    // prints mutex
    private static final Semaphore printMutex = new Semaphore(1,true);
    // meal counterrr
    private static int totalMeals = 0;

    private static int numPhilosophers; //P
    private static int numMeals; //M 

    //---------------------------------------
    // Philosopher Thread
    //---------------------------------------


    static class Philosopher extends Thread {
        //philosophers id number
        private final int id;
        private int mealsRemaining;
        private final Random random = new Random();

        //id constructor
        Philosopher(int id){
            super("Philosopher " + id);
            this.id = id;
            int base = numMeals / numPhilosophers;
            int remainer = numMeals % numPhilosophers;
            this.mealsRemaining = base + (id <= remainer ? 1 : 0);
        }
        //returns 3 to 6 cycles of eating
        private int randCycles(){
            return 3 + random.nextInt(4);
        }

        private void simulateCycles() throws InterruptedException{
            Thread.sleep((long) randCycles() * 50);
        }

        //left chop = id -1
        // right chop = id % P
        private int left() {return id - 1;}
        private int right() {return id % numPhilosophers;}

        //this'll help with string formatting
        private void mutexLog(String dashes, String msg) throws InterruptedException{
            printMutex.acquire();
            System.out.println(dashes + msg);
            printMutex.release();
        }
        // 1.	Sit down at table.
        //2.	Pick up left chopstick.
        //3.	Pick up right chopstick.
        //4.	Begin eating.
        //5.	Continue eating for 3–6 cycles.
        //6.	Put down left chopstick.
        //7.	Put down right chopstick.
        //8.	Begin thinking.
        //9.	Continue thinking for 3–6 cycles.
        //10.	IF all meals have not been eaten, GOTO 2.
        //11.	ELSE leave the table.
        @Override
        public void run() {
            try{
                //prints arrival message
                mutexLog("-", "Philosopher " + id + " starting.");
                arriveCount.release(); //"hello" the philosopher has arrived
                arrivalDone.acquire();//got to wait until all of the philosophers are there
                arrivalDone.release(); //passes token for next person waiting
                //entering the meal loop woohoo
                while(mealsRemaining > 0) {
                    //ok so the goal is for odd philosophers to pick up left chopstick then the right
                    // then for the even number philosophers to pick up right chopstick then the left.
                    boolean odd = (id % 2 == 1);
                    int firstChop = odd ? left() : right();
                    int secondChop = odd ? right() : left();
                    String firstName = odd ? "LEFT" : "RIGHT";
                    String secondName = odd ? "RIGHT" : "LEFT";

                    //ok now we have to check and acquire the first chopstick
                    //we have to check and see if the corresponding chopstick is available for usage
                    boolean isFirst = chops[firstChop].tryAcquire();
                    if (isFirst){
                        mutexLog("---", "Philosopher " + id + "'s " + firstName + " chopstick IS available.");
                    }
                    else{
                        mutexLog("---", "Philosopher " + id + "'s " + firstName + " chopstick IS NOT available.");
                        chops[firstChop].acquire();
                        mutexLog("---", "Philosopher " + id +"'s " + firstName + " chopstick IS available.");
                    }

                    //now we have to do the same thing for the second chopstick

                    boolean isSecond = chops[secondChop].tryAcquire();
                    if(isSecond){
                        mutexLog("---", "Philosopher " + id + "'s " + secondName + " chopstick IS available.");
                    }
                    else{
                        mutexLog("---", "Philosopher " + id + "'s " + secondName + " chopstick IS NOT available");
                        chops[secondChop].acquire();
                        mutexLog("---", "Philosopher " + id + "'s " + secondName + " chopstick IS available.");
                    }

                    //now we have to do when a philosopher has BOTH chopsticks
                    mutexLog("----", "Philosopher " + id + " grabs both chopsticks.");
                    mutexLog("----", "Philosopher " + id + " has a pair of chopsticks.");

                    //now we start with steps 4 and 5
                    mutexLog("-----", "Philosopher " + id + " is eating.");
                    simulateCycles();

                    //while philosopher(s) eats, the total meals counter increments
                    printMutex.acquire();
                    totalMeals++;
                    System.out.println("Meals ate: " + totalMeals);
                    printMutex.release();

                    mealsRemaining--;

                    mutexLog("------", "Philosopher " + id + " is finished eating.");
                    //now we start steps 6 & 7
                    chops[left()].release();
                    mutexLog("-------", "Philosopher " + id + " dropped his left chopstick.");
                    chops[right()].release();
                    mutexLog("-------", "Philosopher " + id + " dropped his right chopstick.");

                    //now we start steps 8 & 9
                    if(mealsRemaining > 0) {
                        mutexLog("--------", "Philosopher " + id + " is thinking...");
                        simulateCycles();
                    }
                    //step 10 will continue to loop until no more meals
                }
                //once meals are all finished, signal in main and wait
                finishedCount.release();
                releaseAll.acquire();
                releaseAll.release();

                //final step 11
                mutexLog("----------", "Philosopher " + id + " has left the table.");

            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
        }
    }
}
public static void main(String args[]) throws InterruptedException{
    Scanner scanner = new Scanner(System.in);
    System.out.println("How many philosophers should be created? (integer from 1 - 10000): ");
    numPhilosophers = scanner.nextInt();
    System.out.println("How many meals should the philosophers eat? (integer from 1 - 10000): ");
    numMeals = scanner.nextInt();
    scanner.close();
    //have to initialize the semaphore values
    chops = new Semaphore[numPhilosophers];
    for (int i = 0; i < numPhilosophers; i++) {
        chops[i] = new Semaphore(1, true);
    }
    //arrival count will start at 0 and each philosopher will release once
    //will acquire P times
    arrivalDone = new Semaphore(0);
    arriveCount = new Semaphore(0);

    //leave count will start at 0 and then each philosopher will release it once done earing
    //will acquire P times
    finishedCount = new Semaphore(0);
    releaseAll = new Semaphore(0);
    //commencing philosopher threads
    Philosopher[] philosophers = new Philosopher[numPhilosophers];
    for (int i = 0; i < numPhilosophers; i++) {
        philosophers[i] = new Philosopher(i + 1);
        philosophers[i].start();
    }
    //arrival loop
    for (int i = 0; i < numPhilosophers; i++) {
        arriveCount.acquire();
    }
    System.out.println("-- All Philosophers have arrived.");
    arrivalDone.release();
    //leave loop
    for (int i = 0; i < numPhilosophers; i++) {
        finishedCount.acquire();
    }
    System.out.println("--------- All Philosophers have finished eating");
    releaseAll.release();

    //waits until all threads exit
    for (int i = 0; i < numPhilosophers; i++) {
        philosophers[i].join();
    }

    }
}
