package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.LinkedList;
import java.util.Map;

public class MultiLevelScheduler extends Scheduler {


    @Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new MultiLevelQueue();
    }

    private class MultiLevelQueue extends ThreadQueue {

        private MultiLevelQueue() {
            shortQueue = new LinkedList<>();
            longQueue = new LinkedList<>();
        }

        private boolean isShort(KThread kThread) {
            if (!startTime.containsKey(kThread) || !endTime.containsKey(kThread)) {
                return false;
            }
            Long s = startTime.get(kThread);
            Long e = endTime.get(kThread);
            return e - s < 1000;
        }

        private boolean isLong(KThread kThread) {
            if (!startTime.containsKey(kThread) || !endTime.containsKey(kThread)) {
                return true;
            }
            Long s = startTime.get(kThread);
            Long e = startTime.get(kThread);
            return e - s >= 1000;
        }

        @Override
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            if (isShort(thread)) shortQueue.add(thread);
            if (isLong(thread)) longQueue.add(thread);
        }

        @Override
        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            KThread kThread = KThread.currentThread();
            Long t = Machine.timer().getTime();
            if (kThread != null) {
                endTime.put(kThread, t);
            }
            KThread ret = null;
            if (!shortQueue.isEmpty()) {
                ret = shortQueue.removeFirst();
            } else if (!longQueue.isEmpty()) {
                ret = longQueue.removeFirst();
            }
            if (ret != null) {
                startTime.put(ret, t);
            }
            return ret;
        }

        @Override
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            Lib.assertTrue(shortQueue.isEmpty() && longQueue.isEmpty());
        }

        @Override
        public void print() {

        }

        LinkedList<KThread> shortQueue, longQueue;
        Map<KThread, Long> startTime, endTime;
    }
}
