package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.Random;

/**
 * A scheduler that chooses threads using a lottery.
 * 
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * 
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * 
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
		super();
	}


	private static final int priorityDefault = 1;

	private static final int priorityMinimum = 1;

	private static final int priorityMaximum = 7;

	/**
	 * Allocate a new lottery thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer tickets from
	 *            waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
	@Override
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new LotteryQueue(transferPriority);
	}

	@Override
	public void setPriority(KThread kThread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
		getThreadState(kThread).setPriority(priority);
	}

	@Override
	public boolean increasePriority() {
		boolean status = Machine.interrupt().disable();
		KThread kThread = KThread.currentThread();

		int priority = getPriority(kThread);

		if (priority == priorityMaximum) {
			Machine.interrupt().restore(status);
			return false;
		}
		setPriority(kThread, ++priority);
		Machine.interrupt().restore(status);
		return true;
	}

	@Override
	public boolean decreasePriority() {
		boolean status = Machine.interrupt().disable();
		KThread kThread = KThread.currentThread();

		int priority = getPriority(kThread);
		if (priority == priorityMinimum) {
			Machine.interrupt().restore(status);
			return false;
		}

		setPriority(kThread, --priority);
		Machine.interrupt().restore(status);
		return true;
	}

	@Override
	protected LotteryState getThreadState(KThread kThread) {
		if (kThread.schedulingState == null) {
			kThread.schedulingState = new LotteryState(kThread);
		}
		return (LotteryState) kThread.schedulingState;
	}

	private class LotteryQueue extends PriorityScheduler.PriorityQueue {
		private Random random = new Random();
		private LotteryQueue(Boolean transferPriority) {
			super(transferPriority);
		}

		@Override
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			if (holder != null) {
				holder.holdList.remove(this);
			}
			LotteryState lotteryState = pickNextThread();
			if (lotteryState != null) {
				lotteryState.acquire(this);
				stateQueue.remove(lotteryState);
				return lotteryState.thread;
			}
			holder = null;
			return null;
		}

		@Override
		protected LotteryState pickNextThread() {
			int priority = 0;
			for (ThreadState threadState : stateQueue) {
				priority += threadState.getEffectivePriority();
			}
			if (priority == 0) return null;
			int c = random.nextInt(priority);
			for (ThreadState threadState : stateQueue) {
				int t = threadState.getEffectivePriority();
				if (c < t) return (LotteryState) threadState;
				c -= t;
			}
			return null;
		}
	}


	private class LotteryState extends PriorityScheduler.ThreadState {
		private LotteryState(KThread kThread) {
			super(kThread);
		}

		@Override
		public void setPriority(int priority) {
			this.priority = priority;
		}

		@Override
		public void waitForAccess(PriorityQueue waitQueue) {
			boolean status = Machine.interrupt().disable();
			waitQueue.stateQueue.add(this);
			belongTo = waitQueue;
			Machine.interrupt().restore(status);
		}

		@Override
		public void acquire(PriorityQueue waitQueue) {
			boolean status = Machine.interrupt().disable();
			waitQueue.holder = this;
			holdList.add(waitQueue);
			belongTo = null;
			Machine.interrupt().restore(status);
		}

		@Override
		public int getEffectivePriority() {
			int ans = priority;
			for (PriorityQueue priorityQueue : holdList) {
				if (priorityQueue.transferPriority) {
					for (ThreadState threadState : priorityQueue.stateQueue) {
						ans += threadState.getEffectivePriority();
					}
				}
			}
			return ans;
		}
	}
}
