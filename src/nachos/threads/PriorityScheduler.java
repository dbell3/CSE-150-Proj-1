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
     * Allocate a new priority thread queue.
     *
     * @param transferPriority <tt>true</tt> if this queue should transfer priority
     *                         from waiting threads to the owning thread.
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
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority + 1);

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
     * @param thread the thread whose scheduling state to return.
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

        /** Adds threads into the queue */
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            /** remove owner from resource list */
            if (owner != null 
            && transferPriority == true)
                owner.lock.remove(this);

            ThreadState st = getThreadState(thread);
            owner = st;

            st.acquire(this);
			Lib.debug(dbgQueue, printQueue());
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());

            if (waitQueue.isEmpty())
                return null;

            // remove owner
            if (owner != null 
            && transferPriority == true)
                owner.lock.remove(this);

            ThreadState st = pickNextThreadState();

            if (st != null) {
                waitQueue.remove(st);
                st.acquire(this);
            }

			Lib.debug(dbgQueue, printQueue());
            return st.thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return, without
         * modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would return.
         */
        protected ThreadState pickNextThreadState() {

			ThreadState temp = waitQueue.element();
			int index = 0, out = 0;

			// Search the whole list and pull the highest
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

        public int getEffectivePriority() {

            if (transferPriority == false) {
                return priorityMinimum;
            }

            if (altered) 
            {
                effectivePriority = priorityMinimum;
                // Search list for highest effectivepriority
                for (ThreadState st : waitQueue) 
                {
                    if (st.getEffectivePriority() > effectivePriority)
                        effectivePriority = st.getEffectivePriority();
                }

                altered = false;
            }

            return effectivePriority;
        }

        public void changed() {
            if (!transferPriority )
                return;

            altered = true;

            if (owner != null)
                owner.changed();
        }

        /**
		 * Printing the Queue for testing purposes
		 * @return String to print on command line
		 */
		public String printQueue() {
			Lib.assertTrue(Machine.interrupt().disabled());

			String temp= "--Priority Ready Queue: " + waitQueue.size() + " items [ ";
            for(ThreadState tstate: waitQueue)
            {
                temp += tstate.thread.getName() + " **EP: " + 
                tstate.getEffectivePriority() + " ct: " +  ", ";
			}
			temp += " ]";

			return temp;
        }

        public void print() {
        }

        private boolean altered;

        public boolean transferPriority;

        /** Keep the highest priority */
        private int effectivePriority;

        /** The queue waiting on this resource */
        private LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>(); 

        /** The owner of the resources */
        private ThreadState owner = null;

    } 


    /**
     * The scheduling state of a thread. This should include the thread's priority,
     * its effective priority, any objects it owns, and the queue it's waiting for,
     * if any.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {

        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param thread the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;

            
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
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority() {

            int maxEffective = this.priority;

            if (altered) {
                for(PriorityQueue pq: lock){
                    int effective = pq.getEffectivePriority();
                    if (maxEffective < effective) {
                        maxEffective = effective;
                    }
                }
            }

            return maxEffective;
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

            changed();
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
            pQueue.changed();

            // set waitingOn
            wait = pQueue;

            // removed pQueue if already in
            if (lock.indexOf(pQueue) != -1) {
                lock.remove(pQueue);
                pQueue.owner = null;
            }
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
        public void acquire(PriorityQueue waitQueue) {

            Lib.assertTrue(Machine.interrupt().disabled());

            // add waitQueue to myResource list
            lock.add(waitQueue);

            // clean waitingOn if waitQueue is just waiting on
            if (waitQueue == wait) {
                wait = null;
            }

            // effective priority may be varied, set dirty flag
            changed();
        }

        /**
         * ThreadState.changed Set the dirty flag, then call setdirty() on each thread
         * of priority queue that the thread is waiting for.
         *
         * ThreadState.changed and PriorityQueue.changed would invoke each other, they
         * are mutually recursive.
         *
         */
        public void changed() {
            if (altered) {
                return;
            }

            altered = true;

            PriorityQueue pg = (PriorityQueue) wait;
            if (pg != null) {
                pg.changed();
            }
        }

        /** The thread with which this object is associated. */
        protected KThread thread;

        /** The priority of the associated thread. */
        protected int priority;

        protected int effectivePriority; 

        /**
         * Collection of PriorityQueues that signify the Locks or other resource that
         * this thread currently holds
         */
        protected LinkedList<PriorityQueue> lock = new LinkedList<PriorityQueue>();

        /**
         * PriorityQueue corresponding to resources that this thread has attepmted to
         * acquire but could not
         */
        protected PriorityQueue wait;

        /**
         * Set to true when this thread's priority is changed, or when one of the queues
         * in myResources flags itself as dirty
         */
        private boolean altered = false; 
    }
	private static final char dbgQueue = 'q';
}