package nachos.threads; 
import nachos.machine.*;

import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to starve a thread if there's always a thread waiting 
 * with higher priority.
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
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
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
				
		Lib.assertTrue(priority >= priorityMinimum &&
			priority <= priorityMaximum);
		
		getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
				
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

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
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
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

		// adds the thread into the queue
		public void waitForAccess(KThread thread) {
			//Disable interrupts
			Lib.assertTrue(Machine.interrupt().disabled());

			getThreadState(thread).waitForAccess(this);

			// Lib.debug(dbgQueue, thread.getName() + " release lock");
			Lib.debug(dbgQueue, printQueue());
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			// If waitQueue is empty
			if (waitQueue.isEmpty())
				return null;

			KThread thread = pickNextThread().thread;

			Lib.debug(dbgQueue, printQueue());
			// Lib.debug(dbgQueue, thread.getName() + " grabing lock");
			return thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return, without
		 * modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			// Make sure that the Queue is not empty
			if (waitQueue.isEmpty())
				// If it is then return null
				return null;

			ThreadState temp = waitQueue.element();
			ThreadState starvingThread = temp;
			int index = 0, out = 0;

			// int currentTime = (int) Machine.timer().getTime();
			// Search the whole list and pull the highest
			for (ThreadState threadState : waitQueue) {
				// find the starving thread 
				if(threadState.createdTime < starvingThread.createdTime && 
				threadState.priority < starvingThread.priority){
					starvingThread = threadState;
				}

				if(threadState.getEffectivePriority() > temp.getEffectivePriority()){
					temp = threadState;
					out = index;
				}
				index++;
			}

			// Increase priority of starving thread 
			if(starvingThread.getEffectivePriority() <= priorityMaximum && transferPriority)
				starvingThread.setPriority(priorityMaximum);
				// starvingThread.setPriority(starvingThread.getEffectivePriority()+1);

			// Remove ThreadState from queue
			waitQueue.remove(out);

			return temp;
		}

		/**
		 * Printing the Queue for testing purposes
		 * @return String to print on command line
		 */
		public String printQueue() {
			Lib.assertTrue(Machine.interrupt().disabled());

			String temp= "--Priority Ready Queue: " + waitQueue.size() + " items [ ";
			for(ThreadState tstate: waitQueue){
				temp += tstate.thread.getName() + " **EP: " + tstate.getEffectivePriority() + " ct: " +  tstate.createdTime + ", ";
			}
			temp += " ]";

			return temp;
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting threads to
		 * the owning thread.
		 */
		public boolean transferPriority;

		/** Queue to that keeps the threads */
		private LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>();

		// @Override
		public void print() {
			// TODO Auto-generated method stub

		}
	}

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
	// protected class ThreadState implements Comparable {
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			// setting random Priorities for testing purposes
			if(Lib.test(dbgQueue))
				setPriority((int)(Math.random()*7)+1);
			else
				setPriority(priorityDefault);

			this.createdTime = (int) Machine.timer().getTime();
		}

		/**
		 * CompareTo method is needed in order to use TreeSet
		 * We want to use a Treeset because it automatically sorts
		 * by highest priority
		 */
		public int compareTo(Object o) {
			ThreadState tstate = (ThreadState) o;

			if (priority < tstate.priority)
				return -1;
			else if (priority > tstate.priority)
				return 1;
			else return 0; 
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			Lib.assertTrue(Machine.interrupt().disabled());
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
		
			// System.out.println(Machine.timer().getTime());
			
			// int effectivePriority;
			// if (!transferPriority) {
			// 	return priorityMinimum;
			// }else {
			// 	effectivePriority = priority;
			// 	for(ThreadState threadState : waitQueue) {
			// 		effectivePriority = threadState.priority;
			// 	}
			// 	transferPriority = false;
			// 	return effectivePriority;
			// }
			return priority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is the
		 * associated thread) is invoked on the specified priority queue. The associated
		 * thread is therefore waiting for access to the resource guarded by
		 * <tt>waitQueue</tt>. This method is only called if the associated thread
		 * cannot immediately obtain access.
		 *
		 * @param waitQueue the queue that the associated thread is now waiting on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue pQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());

			pQueue.waitQueue.add(this);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is guarded
		 * by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue pQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());

			Lib.assertTrue(pQueue.waitQueue.isEmpty());
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;

		protected int createdTime;
	}

	private static final char dbgQueue = 'q';
}