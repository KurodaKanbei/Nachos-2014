package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat {

    public static void selfTest() {
        //System.out.println("\n ***Testing Boats with only 1 children***");
        //new BoatGrader().startTest(0, 1);

        System.out.println("\n ***Testing Boats with only 2 children***");
        new BoatGrader().boatTest(0, 2);

        System.out.println("\n ***Testing Boats with only 3 children***");
        new BoatGrader().boatTest(0, 3);

        System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        new BoatGrader().boatTest(1, 2);

        //System.out.println("\n ***Testing Boats with 3 children, 3 adult***");
        //new BoatGrader().boatTest(100, 2);

        //System.out.println("\n ***Testing Boats with 10 children, 10 adult***");
        //new BoatGrader().startTest(10, 10);
    }

    public static void begin(int adults, int children, BoatGrader b) {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;

        // Instantiate global variables here

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        childrenOnBoard = 0;
        adultsOnA = adults;
        childrenOnA = children;
        boatA = true;
        Lib.debug('b', "boat test start");
        for (int i = 0; i < adults; i++) {
            new KThread(Boat::AdultItinerary).setName("Adult" + i).fork();
        }
        for (int i = 0; i < children; i++) {
            new KThread(Boat::ChildItinerary).setName("Child" + i).fork();
        }
        done.P();
    }

    private static void AdultItinerary() {
        /*
         * This is where you should put your solutions. Make calls to the
         * BoatGrader to show that it is synchronized. For example:
         * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
         * across to Molokai
         */
        islandA.acquire();
        while (!(childrenOnA <= 1 && boatA)) {
            adultsA.sleep();
        }
        //critical region start
        System.out.println(KThread.currentThread() + " on board");
        bg.AdultRowToMolokai();
        boatA = false;
        --adultsOnA;
        islandA.release();
        islandB.acquire();
        childrenB.wake();
        islandB.release();
        //critical region end
    }

    private static void ChildItinerary() {
        while (adultsOnA + childrenOnA > 1) {
            islandA.acquire();
            if (adultsOnA > 0) {
                adultsA.wake();
            }
            while (!(childrenOnBoard < 2 && boatA)) {
                childrenA.sleep();
            }
            if (childrenOnBoard == 1) {
                System.out.println(KThread.currentThread() + " on board as passenger");
                ++childrenOnBoard;
                bg.ChildRowToMolokai();
                childrenOnBoat.wake();
                childrenOnBoat.sleep();
            } else if (childrenOnBoard == 0) {
                System.out.println(KThread.currentThread() + " on board as captain");
                ++childrenOnBoard;
                childrenA.wake();
                childrenOnBoat.sleep();
                bg.ChildRideToMolokai();
                Lib.debug('b', " two children to B");
                childrenOnBoat.wake();
            }
            --childrenOnBoard;
            --childrenOnA;
            boatA = false;
            islandA.release();
            islandB.acquire();
            if (childrenOnBoard == 1) {
                childrenB.sleep();
            }
            System.out.println(KThread.currentThread() + " on board as the sole man");
            islandB.release();
            bg.ChildRowToOahu();
            Lib.debug('b', " one child to A");
            islandA.acquire();
            ++childrenOnA;
            boatA = true;
            islandA.release();
        }
        Lib.debug('b', " one child to B");
        islandA.acquire();
        --childrenOnA;
        islandA.release();
        System.out.println(KThread.currentThread() + "on board");
        bg.ChildRowToMolokai();
        islandB.acquire();
        islandB.release();
        done.V();
    }


    private static BoatGrader bg;
    private static int childrenOnA, adultsOnA, childrenOnBoard;
    private static Lock islandA = new Lock(), islandB = new Lock();
    private static Condition2 adultsA = new Condition2(islandA);
    private static Condition2 childrenA = new Condition2(islandA), childrenB = new Condition2(islandB);
    private static Condition2 childrenOnBoat = new Condition2(islandA);
    private static Semaphore done = new Semaphore(0);
    private static boolean boatA;
}
