package nachos.threads; 
import nachos.machine.*;

import java.util.LinkedList;

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

			// Make a thread state from the thread
			ThreadState st = getThreadState(thread);

			if(st == owner && transferPriority)
				return;

			// Add the new state to the wait queue 
			waitQueue.add(st);

			if(st.getEffectivePriority() > owner.getEffectivePriority())
				owner.setEffectivePriority(st.getEffectivePriority());

			// Lib.debug(dbgQueue, thread.getName() + " release lock");
			Lib.debug(dbgQueue, printQueue());
		}

		public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
			
			ThreadState st = getThreadState(thread);

			if(!transferPriority)			
				waitQueue.add(st);
			else
				owner = st;
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			KThread thread;

			if(transferPriority)
			{
				thread = owner.thread;
				acquire(pickNextThread().thread);
			}
			else
				thread = pickNextThread().thread;


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
				return null;

			ThreadState temp = waitQueue.element();
			int index = 0, out = 0;
			// Search the whole list and pull the highest effective priority
			for (ThreadState threadState : waitQueue) {

				if(threadState.getEffectivePriority() > temp.getEffectivePriority()){
					temp = threadState;
					out = index;
				}
				index++;
			}

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

		/*****************************/
		/**  PriorityQueue variables */
		/*****************************/
		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting threads to
		 * the owning thread.
		 */
        public boolean transferPriority;
        
        /** owner of Queue */
		public ThreadState owner = null;
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
		public int getEffectivePriority() 
		{
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			return effectivePriority > priority ? effectivePriority : priority; 
		}

		public int setEffectivePriority(int newPriority) 
		{
			if(effectivePriority < newPriority)
				effectivePriority = newPriority;

			return effectivePriority;
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

		// /**
		//  * Called when the associated thread has acquired access to whatever is guarded
		//  * by <tt>waitQueue</tt>. This can occur either as a result of
		//  * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		//  * <tt>thread</tt> is the associated thread), or as a result of
		//  * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		//  *
		//  * @see nachos.threads.ThreadQueue#acquire
		//  * @see nachos.threads.ThreadQueue#nextThread
		//  */
		// public void acquire(PriorityQueue pQueue) {
		// 	Lib.assertTrue(Machine.interrupt().disabled());

		// 	Lib.assertTrue(pQueue.waitQueue.isEmpty());
		// }

		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		protected int priority;

		protected int effectivePriority = 0;

		protected int createdTime;
	}

	private static final char dbgQueue = 'q';
}