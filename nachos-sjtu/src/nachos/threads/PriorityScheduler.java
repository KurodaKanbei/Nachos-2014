package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum) {
			Machine.interrupt().restore(intStatus); // bug identified by Xiao Jia @ 2011-11-04
			return false; // indent corrected by Yuda Fan @ 2018-06-20
		}

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum) {
		  	Machine.interrupt().restore(intStatus); // bug identified by Xiao Jia @ 2011-11-04
			return false;
		}

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			if (holder != null) {
				holder.holdList.remove(this);
				holder.calcEffectivePriority();
			}
			ThreadState threadState = pickNextThread();
			if (threadState != null) {
				threadState.acquire(this);
				stateQueue.remove(threadState);
				return threadState.thread;
			}
			holder = null;
			return null;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */

		private ThreadState getSuperior(ThreadState x, ThreadState y) {
			Lib.assertTrue(x != null);
			if (y == null) return x;
			if (x.effectivePriority > y.effectivePriority ||
					x.effectivePriority == y.effectivePriority && x.enterTime < y.enterTime) {
				return x;
			}
			return y;
		}

		protected ThreadState pickNextThread() {
			ThreadState ret = null;
			for (ThreadState threadState : stateQueue) {
				ret = getSuperior(threadState, ret);
			}
			return ret;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			for (ThreadState threadState : stateQueue) {
				System.out.println("Thread : " + threadState.getThread() + " Priority: " + threadState.getPriority()
						+ " effectivePriority = " + threadState.getEffectivePriority());
			}
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		protected boolean transferPriority;
		protected ThreadState holder = null;
		protected LinkedList<ThreadState> stateQueue = new LinkedList<>();
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {

		public KThread getThread() {
			return thread;
		}

		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */


		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			return effectivePriority;
		}

		protected void updateEffectivePriority(int p) {
			if (p > effectivePriority) {
				effectivePriority = p;
				if (belongTo != null && belongTo.holder != null) {
					belongTo.holder.updateEffectivePriority(p);
				}
			}
		}

		protected void calcEffectivePriority() {
			int t = effectivePriority;
			effectivePriority = priority;
			for (PriorityQueue priorityQueue : holdList) {
				if (priorityQueue.transferPriority) {
					for (ThreadState threadState : priorityQueue.stateQueue) {
						effectivePriority = Math.max(effectivePriority, threadState.getEffectivePriority());
					}
				}
				if (t != effectivePriority && belongTo != null && belongTo.holder != null) {
					belongTo.holder.calcEffectivePriority();
				}
			}
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			int t = this.priority;
			this.priority = priority;
			if (t == effectivePriority) {
				calcEffectivePriority();
			} else if (priority > t) {
				updateEffectivePriority(priority);
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			boolean status = Machine.interrupt().disable();
			waitQueue.stateQueue.add(this);
			belongTo = waitQueue;
			enterTime = Machine.timer().getTime();
			if (belongTo.holder != null) {
				belongTo.holder.updateEffectivePriority(effectivePriority);
			}
			Machine.interrupt().setStatus(status);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			boolean status = Machine.interrupt().disable();
			waitQueue.holder = this;
			holdList.add(waitQueue);
			belongTo = null;
			calcEffectivePriority();
			Machine.interrupt().restore(status);
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority = priorityDefault;
		protected int effectivePriority;
		protected LinkedList<PriorityQueue> holdList = new LinkedList<>();
		protected PriorityQueue belongTo;
		protected long enterTime;

	}
}
