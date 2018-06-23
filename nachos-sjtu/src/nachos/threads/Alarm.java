package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(this::timerInterrupt);
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean status = Machine.interrupt().disable();
		while (!threadQueue.isEmpty()) {
			WaitThread waitThread = threadQueue.peek();
			assert waitThread != null;
			if (waitThread.getWakeTime() <= Machine.timer().getTime()) {
				waitThread.getKThread().ready();
				threadQueue.poll();
				continue;
			}
			break;
		}
		Machine.interrupt().restore(status);
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x
	 *            the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		boolean status = Machine.interrupt().disable();
		threadQueue.add(new WaitThread(KThread.currentThread(), Machine.timer().getTime() + x));
		KThread.sleep();
		Machine.interrupt().restore(status);
	}

	private class WaitThread implements Comparable<WaitThread> {
		private KThread kThread;
		private long wakeTime;

		public WaitThread(KThread kThread, long wakeTime) {
			this.kThread = kThread;
			this.wakeTime = wakeTime;
		}

		public KThread getKThread() {
			return kThread;
		}

		public long getWakeTime() {
			return wakeTime;
		}

		@Override
		public int compareTo(WaitThread rhs) {
			return	Long.compare(wakeTime, rhs.getWakeTime());
		}
	}

	private PriorityQueue<WaitThread> threadQueue = new PriorityQueue<>();
}
