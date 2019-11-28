package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

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
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {

    LotteryScheduler()
    {
        priorityMinimum = 1;
        priorityMaximum = Integer.MAX_VALUE;
    }

    public static void selfTest()
    {
        boolean machine_start_status = Machine.interrupt().disabled();
        Machine.interrupt().disable();

        LotteryScheduler ls = new LotteryScheduler();
        ThreadQueue tq = ls.newThreadQueue(true);

        KThread t1 = new KThread();
        t1.setName("t1");
        KThread t2 = new KThread();
        t2.setName("t2");

        KThread t3 = new KThread();
        t3.setName("t3");
        KThread t4 = new KThread();
        t4.setName("t4");
        KThread t5 = new KThread();
        t5.setName("t5");

        tq.acquire(t1);
        tq.waitForAccess(t2);
        tq.waitForAccess(t3);
        tq.waitForAccess(t4);
        tq.waitForAccess(t5);

        // This could be done with a loop, but I'm to lazy
        // int random = (int) Math.random() * 10;
        ls.setPriority(t1,10);
        // random = (int) Math.random() * 40;
        ls.setPriority(t2,15);
        // random = (int) Math.random() * 239;
        ls.setPriority(t3,30);
        // random = (int) Math.random() * 5;
        ls.setPriority(t4,160);

        KThread thread = tq.nextThread();
        while(thread != null)
        {
            System.out.print(thread.getName() + " ");
            thread = tq.nextThread();
        }
        System.out.println("\n");

        Machine.interrupt().restore(machine_start_status);
    }
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
    }

    public class LotteryQueue extends PriorityQueue
    {
        LotteryQueue(boolean transferPriority)
        {
            super(transferPriority);
            this.transferPriority = transferPriority;
        }

        @Override
        protected ThreadState pickNextThreadState() 
        {
			// Add tickets to the owner 
			for (ThreadState st: waitQueue) {
                owner.effectivePriority += st.getEffectivePriority();
			}

            int lottery = (int)(Math.random()*waitQueue.size());

			// Remove ThreadState from queue
            return waitQueue.get(lottery);
        }
    }
}
