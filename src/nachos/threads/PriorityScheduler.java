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
    public class PriorityQueue extends ThreadQueue {
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        /** Adds threads into the queue */
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        /**
         * If transferPriority is true then
         * make the Kthread the owner
         */
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            /** remove owner from resource list */
            if (owner != null 
            && transferPriority == true)
                owner.threadStatePQ.remove(this);

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
            // if (owner != null 
            // && transferPriority == true)
            //     owner.threadStatePQ.remove(this);

            ThreadState st = pickHighestEffectiveState();

            if(owner.getEffectivePriority() < st.getEffectivePriority())
            {
                owner.effectivePriority = st.effectivePriority;
                return owner.thread;
            }

            if (st != null && transferPriority) {
                waitQueue.remove(st);
                st.acquire(this);
            }

			Lib.debug(dbgQueue, printQueue());
            return st.thread;
        } // PriorityQueue nextThread

        protected ThreadState pickHighestEffectiveState() 
        {
            Lib.assertTrue(Machine.interrupt().disabled());

			ThreadState temp = waitQueue.element();
			int index = 0, out = 0;

			// Search the whole list and pull the highest
            for (ThreadState st: waitQueue) 
            {
                if(st.getEffectivePriority() > temp.getEffectivePriority())
                {
					temp = st;
					out = index;
				}
				index++;
			}

			// Remove ThreadState from queue
            waitQueue.remove(out);
            
            return temp;
        }

        public int getEffectivePriority() 
        {
            if (transferPriority == false)
                return priorityMinimum;

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
        protected LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>(); 

        /** The owner of the resources */
        public ThreadState owner = null;

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
            Lib.assertTrue(Machine.interrupt().disabled());
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
            Lib.assertTrue(Machine.interrupt().disabled());
            return priority;
        }

        public int getEffectivePriority() {

            Lib.assertTrue(Machine.interrupt().disabled());
            int maxEffective = this.priority;

            if (altered) {
                for(PriorityQueue pq: threadStatePQ)
                {
                    int effective = pq.getEffectivePriority();
                    if (maxEffective < effective)
                        maxEffective = effective;
                }
            }// ThreadState getEffectivePriority

            return maxEffective;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param priority the new priority.
         */
        public void setPriority(int priority) {
            Lib.assertTrue(Machine.interrupt().disabled());
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

            // Add this thread state to the Priority wait queue
            pQueue.waitQueue.add(this);
            pQueue.changed();

            // set waitingOn
            wait = pQueue;

            // removed pQueue if already in
            if (threadStatePQ.indexOf(pQueue) != -1) {
                threadStatePQ.remove(pQueue);
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

            threadStatePQ.add(waitQueue);

            if (waitQueue == wait)
                wait = null;

            changed();
        }

        public void changed() {
            if (altered) {
                return;
            }

            altered = true;

            PriorityQueue threadStatePQ = (PriorityQueue) wait;
            if (threadStatePQ != null) {
                threadStatePQ.changed();
            }
        }

        /******************************/
        /**  ThreadState variables  **/
        /****************************/
        protected KThread thread;

        protected int priority;

        protected int effectivePriority; 

        protected LinkedList<PriorityQueue> threadStatePQ = new LinkedList<PriorityQueue>();

        protected PriorityQueue wait;

        private boolean altered = false; 
    }

    private static final char dbgQueue = 'q';
}