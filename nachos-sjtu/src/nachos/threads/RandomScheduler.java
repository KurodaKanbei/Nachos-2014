package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.HashSet;
import java.util.Random;

public class RandomScheduler extends Scheduler {

    @Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new RandomQueue();
    }

    private class RandomQueue extends ThreadQueue {
        private HashSet<HashThread> waitSet = new HashSet<>();

        @Override
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            waitSet.add(new HashThread(thread));
        }

        @Override
        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());

            if (waitSet.isEmpty()) return null;

            HashThread hashThread = waitSet.iterator().next();
            KThread kThread = hashThread.kThread;
            waitSet.remove(hashThread);
            return kThread;
        }

        @Override
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            Lib.assertTrue(waitSet.isEmpty());
        }

        @Override
        public void print() {

        }
    }

    private class HashThread {
        KThread kThread;
        int d;

        private HashThread(KThread kThread) {
            this.d = random.nextInt();
            this.kThread = kThread;
        }
    }

    private static Random random = new Random();
}
