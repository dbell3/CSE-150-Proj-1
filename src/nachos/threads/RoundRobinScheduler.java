package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
import java.util.Iterator;

/**
 * A round-robin scheduler tracks waiting threads in FIFO queues, implemented
 * with linked lists. When a thread begins waiting for access, it is appended
 * to the end of a list. The next thread to receive access is always the first
 * thread in the list. This causes access to be given on a first-come
 * first-serve basis.
 */
public class RoundRobinScheduler extends Scheduler {
    /**
     * Allocate a new round-robin scheduler.
     */
    public RoundRobinScheduler() {
    }
    
    /**
     * Allocate a new FIFO thread queue.
     *
     * @param	transferPriority	ignored. Round robin schedulers have
     *					no priority.
     * @return	a new FIFO thread queue.
     */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new FifoQueue();
	}

	private class FifoQueue extends ThreadQueue {
		/**
		 * Add a thread to the end of the wait queue.
		 *
		 * @param thread the thread to append to the queue.
		 */
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());

			waitQueue.add(thread);

			// print();
		}

		/**
		 * Remove a thread from the beginning of the queue.
		 *
		 * @return the first thread on the queue, or <tt>null</tt> if the queue is
		 *         empty.
		 */
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			// Make sure that the Queue is not empty
			if (waitQueue.isEmpty())
				// If it is then return null
				return null;

			// Remove and return the first item in the Queue
			KThread firstItem = waitQueue.removeFirst();

			Lib.debug(dbgThread, firstItem.getName() + " Pop from the Queue");
			print();
			return firstItem;
		}

		/**
		 * The specified thread has received exclusive access, without using
		 * <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no threads are
		 * waiting for access.
		 */
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());

			Lib.assertTrue(waitQueue.isEmpty());
		}

		/**
		 * Print out the contents of the queue.
		 */
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.debug(dbgThread, "--Ready Queue: " + waitQueue.size() + " items");

			System.out.print("[ ");
			for (Iterator i = waitQueue.iterator(); i.hasNext();)
				System.out.print((KThread) i.next() + " ,");

			System.out.println(" ]");
		}


		private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
		// For printing debuging messages about threads
		private static final char dbgThread = 'q';
	}
}
