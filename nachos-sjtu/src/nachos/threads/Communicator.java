package nachos.threads;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();
		use = false;
		spe = new Condition2(lock);
		lis = new Condition2(lock);
		listener = 0;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		while (use || listener == 0) spe.sleep();
		this.word = word;
		use = true;
		lis.wake();
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();
		++listener;
		spe.wake();
		while (!use) lis.sleep();
		int ret = word;
		use = false;
		--listener;
		spe.wake();
		lock.release();
		return ret;
	}

	private Lock lock;
	private int word, listener;
	private boolean use;
	private Condition2 spe, lis;
}
