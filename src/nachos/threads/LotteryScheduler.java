package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() 
    {
        priorityDefault = 1;
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
        ls.setPriority(t1,1);
        // random = (int) Math.random() * 40;
        ls.setPriority(t2,1);
        // random = (int) Math.random() * 239;
        ls.setPriority(t3,1);
        // random = (int) Math.random() * 5;
        ls.setPriority(t4,1);

        KThread thread = tq.nextThread();
        while(thread != null)
        {
            System.out.print(thread.getName() + " ");
            thread = tq.nextThread();
        }
        System.out.println("\n");

        Machine.interrupt().restore(machine_start_status);
    }


    @Override
    protected ThreadState getThreadState(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        if (thread.schedulingState == null)
            thread.schedulingState = new LotteryThreadState(thread);

        return (ThreadState) thread.schedulingState;
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
        return new LotteryQueue(transferPriority);
    }


    protected class LotteryQueue extends PriorityQueue{
        LotteryQueue(boolean transferPriority)
        {
            // this.transferPriority = transferPriority;
            super(transferPriority);
        }

        private class ticketInterval{
            ThreadState ticketOwner;
            int startInterval, endInterval;
            ticketInterval(ThreadState ticketOwner, int startInterval, int endInterval)
            {
                this.ticketOwner = ticketOwner;
                this.startInterval = startInterval;
                this.endInterval = endInterval;
            }

            public boolean contains(int ticket) {
                return (startInterval < ticket && ticket < endInterval) || startInterval == ticket || endInterval == ticket;
            }
        }

        @Override
        public ThreadState pickNextState()
        {
            // if (waitQueue.isEmpty() > 0)
            //     return waitQueue.getLast();

            if (waitQueue.isEmpty())
             return null;

            Iterator<ThreadState> it = waitQueue.iterator();
            int maxRange = 0;
            HashSet<ticketInterval> threadTicketStates = new HashSet<ticketInterval>();
           


            while(it.hasNext())
            {
                ThreadState next = it.next();
                threadTicketStates.add(new ticketInterval(next,maxRange + 1,maxRange + next.getEffectivePriority()));

                maxRange += next.getEffectivePriority();
            }

            Random rand = new Random();
            int ticket = rand.nextInt((maxRange - 1) + 1) + 1;
            Iterator<ticketInterval> it2 = threadTicketStates.iterator();

            while(it2.hasNext())
            {
                ticketInterval temp = it2.next();

                if (temp.contains(ticket))
                {
                    waitQueue.remove(temp.ticketOwner);
                    return temp.ticketOwner;
                }
            }

            return null;
        }

        @Override
        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            if (waitQueue.isEmpty())
                return null;

            ThreadState tState = pickNextState();

            KThread thread = tState.thread;

            if (thread != null)
            {
                if (this.owner != null)
                {
                    //Remove this from the old owners' queue
                    this.owner.thWaiting.remove(this);


                    Iterator<ThreadState> it = waitQueue.iterator();
                    while(it.hasNext() && transferPriority)
                    {
                        ThreadState temp = it.next();
                        this.owner.effectivePriority -= temp.getEffectivePriority();
                    }
                    
                    this.owner.changed();

                }

                //Now thread is going to run, so it should acquire this waitQueue and it shouldnt be waiting on anything else
                ((ThreadState)thread.schedulingState).thWaiting = null;
                // ((ThreadState) thread.schedulingState).acquire(this);
            }

            return thread;
        }

        Boolean transferPriority = false;
    }

    protected class LotteryThreadState extends ThreadState
    {
        LotteryThreadState(KThread thread)
        {
            super(thread);
            // priorityCopy = priority;
            // effectivePriorityCopy = effectivePriority;
        }

        // @Override
        // public void waitForAccess(PriorityQueue waitQueue)
        // {
        //     Lib.assertTrue(Machine.interrupt().disabled());
        //     // Lib.assertTrue(waitingQueue == null);

        //     waitQueue.waitQueue.add(this);
        //     // thWaiting = waitQueue;

        //    if (waitQueue.owner != null && waitQueue.transferPriority)
        //     {
        //         waitQueue.owner.effectivePriority += getEffectivePriority();
        //         waitQueue.owner.changed();
        //     }

        // }

        // @Override
        // public void acquire(PriorityQueue waitQueue) {
        //     Lib.assertTrue(Machine.interrupt().disabled());
        //     if (waitQueue.owner != null)
        //         waitQueue.owner.ownedQueues.remove(waitQueue);

        //     waitQueue.owner = this;
        //     ownedQueues.add(waitQueue);

        //     Iterator<ThreadState> it =  waitQueue.threadStates.iterator();

        //     while(it.hasNext() && waitQueue.transferPriority)
        //     {
        //         ThreadState temp = it.next();
        //         effectivePriority += temp.getWinningPriority();
        //     }

        //     if (effectivePriority != effectivePriorityCopy)
        //         update();

        // }

        // @Override
        // public void setPriority(int priority) {
        //     Lib.assertTrue(Machine.interrupt().disabled());
        //     if (this.priority == priority)
        //         return;

        //     this.priority = priority;
        //     recalculateThreadScheduling();
        //     update();
        // }

        // @Override
        // public void update()
        // {
        //     if (priorityCopy != priority)
        //     {
        //         effectivePriority = effectivePriority + (priority - priorityCopy);

        //         priorityCopy = priority;

        //         if (waitingQueue != null)
        //             recalculateThreadScheduling();
        //     }

        //     if (waitingQueue == null || waitingQueue.owner == null)
        //     {
        //         effectivePriorityCopy = effectivePriority;
        //         return;
        //     }


        //     if (waitingQueue.transferPriority && effectivePriorityCopy != effectivePriority)
        //     {
        //         waitingQueue.owner.effectivePriority += (effectivePriority - effectivePriorityCopy);
        //         effectivePriorityCopy = effectivePriority;
        //         waitingQueue.owner.recalculateThreadScheduling();
        //         waitingQueue.owner.update();
        //     }
        //     else if (effectivePriority != effectivePriorityCopy)
        //         effectivePriorityCopy = effectivePriority;

        // }

        // These copy values are made so as to check inside the update function of the threadstate
        // if there are necessary changes to be made.
        // For example, if priorityCopy value and priority value are the same than no longer recursive calls
        // need to be made. But if there is a difference then necessary steps need to be made to update the owner
        // of the queue.
        // int priorityCopy;
        // int effectivePriorityCopy;
    }
}