package nachos.ag;

import nachos.machine.Lib;
import nachos.threads.Boat;
import nachos.threads.KThread;

import java.util.Random;

public class BoatGrader extends BasicTestGrader{

    /**
     * BoatGrader consists of functions to be called to show that your solution
     * is properly synchronized. This version simply prints messages to standard
     * out, so that you can watch it. You cannot submit this file, as we will be
     * using our own version of it during grading.
     *
     * Note that this file includes all possible variants of how someone can get
     * from one island to another. Inclusion in this class does not imply that
     * any of the indicated actions are a good idea or even allowed.
     */

    public void run() {
        final int adults = getIntegerArgument("adults");
        final int children = getIntegerArgument("children");
        Lib.assertTrue(adults >= 0 && children >= 0, "number must be non-negative");
        this.startTest(adults, children);
        allCrossed();
        allIsDone();
    }

    public void startTest(int adults, int children) {
        adultsOahu = adults;
        childrenOahu = children;
        adultsMolokai = childrenMolokai = 0;
        Boat.begin(adults, children, this);
    }

    private void allCrossed() {
        Lib.assertTrue(adultsOahu == 0, "there are still " + adultsOahu + " at Oahu\n");
        Lib.assertTrue(adultsMolokai == 0, "there are still " + adultsMolokai + " at Molokai\n");
    }

    private void allIsDone() {

    }

    private void pretendYield() {
        while (random.nextBoolean()) {
            KThread.yield();
        }
    }
    /*
     * ChildRowToMolokai should be called when a child pilots the boat from Oahu
     * to Molokai
     */
    public void ChildRowToMolokai() {
        pretendYield();
        System.out.println("**Child rowing to Molokai.");
        Lib.assertTrue(childrenOahu > 0, "No children in Oahu, can't row to Molokai");
        --childrenOahu;
        ++childrenMolokai;
    }

    /*
     * ChildRowToOahu should be called when a child pilots the boat from Molokai
     * to Oahu
     */
    public void ChildRowToOahu() {
        pretendYield();
        System.out.println("**Child rowing to Oahu.");
        Lib.assertTrue(childrenMolokai > 0, "No children in Molokai, can't row to Oahu");
        --childrenMolokai;
        ++childrenOahu;
    }

    /*
     * ChildRideToMolokai should be called when a child not piloting the boat
     * disembarks on Molokai
     */
    public void ChildRideToMolokai() {
        pretendYield();
        System.out.println("**Child arrived on Molokai as a passenger.");
        Lib.assertTrue(childrenOahu > 0, "No children in Oahu, can't ride to Molokai");
        --childrenOahu;
        ++childrenMolokai;
    }

    /*
     * ChildRideToOahu should be called when a child not piloting the boat
     * disembarks on Oahu
     */
    public void ChildRideToOahu() {
        pretendYield();
        System.out.println("**Child arrived on Oahu as a passenger.");
        Lib.assertTrue(childrenMolokai > 0, "No children in Molokai, can't ride to Oahu");
        ++childrenOahu;
        --childrenMolokai;
    }

    /*
     * AdultRowToMolokai should be called when a adult pilots the boat from Oahu
     * to Molokai
     */
    public void AdultRowToMolokai() {
        pretendYield();
        System.out.println("**Adult rowing to Molokai.");
        Lib.assertTrue(adultsOahu > 0, "No adults in Oahu, can't row to Molokai");
        --adultsOahu;
        ++adultsMolokai;
    }

    /*
     * AdultRowToOahu should be called when a adult pilots the boat from Molokai
     * to Oahu
     */
    public void AdultRowToOahu() {
        pretendYield();
        System.out.println("**Adult rowing to Oahu.");
        Lib.assertTrue(adultsMolokai > 0, "No adults in Molokai, can't row to Oahu");
        ++adultsOahu;
        --adultsMolokai;
    }

    /*
     * AdultRideToMolokai should be called when an adult not piloting the boat
     * disembarks on Molokai
     */
    public void AdultRideToMolokai() {
        Lib.assertNotReached("Invalid Operation Adult Ride to Molokai");
        System.out.println("**Adult arrived on Molokai as a passenger.");
    }

    /*
     * AdultRideToOahu should be called when an adult not piloting the boat
     * disembarks on Oahu
     */
    public void AdultRideToOahu() {
        Lib.assertNotReached("Invalid Operation Adult Ride to Oahu");
        System.out.println("**Adult arrived on Oahu as a passenger.");
    }

    @Override
    public void readyThread(KThread thread) {
        if (thread == idleThread) {
            ++idleReadyCount;
            if (idleReadyCount > 1000) {
                allCrossed();
                allIsDone();
            }
            return;
        }
        idleReadyCount = 0;
    }

    @Override
    public void setIdleThread(KThread idleThread) {
        this.idleThread = idleThread;
    }

    private int adultsOahu, adultsMolokai, childrenOahu, childrenMolokai;
    private KThread idleThread;
    private Random random = new Random();
    private static int idleReadyCount = 0;
}

