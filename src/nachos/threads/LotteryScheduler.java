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

    public static void selfTest()
    {
        boolean machine_start_status = Machine.interrupt().disabled();
        Machine.interrupt().disable();

        LotteryScheduler ls = new LotteryScheduler();
        ThreadQueue tq = ls.newThreadQueue(true);

        KThread k1 = new KThread();
        KThread k2 = new KThread();

        tq.acquire(k1);
        tq.waitForAccess(k2);

        ls.setPriority(k2,67519053);

        KThread thread = tq.nextThread();
        System.out.println(thread.getName() + "\n ");
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
	// implement me
	return null;
    }
}
