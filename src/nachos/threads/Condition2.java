package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	    this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	    conditionLock.release();

        boolean interrupt = Machine.interrupt().disable();

        //Add current thread to sleeping Queue
        sleepQueue.add(KThread.currentThread());

        //Sleeps current thread.
        //By default, KThread's sleep method sleeps current thread. No need to specify
        KThread.sleep();

        Machine.interrupt().restore(interrupt);

	    conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        //Check if queue contains sleeping threads
        if(!sleepQueue.isEmpty()){
            
            boolean interrupt = Machine.interrupt().disable();

            //KThread t = sleepQueue.remove(0);
            KThread t = sleepQueue.pop();
            if(t != null) t.ready();

            Machine.interrupt().restore(interrupt);
        }
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        while(!sleepQueue.isEmpty()){
            wake();
        }
    }

    private Lock conditionLock;

    //Global queue for sleeping threads
    //private Arraylist<KThread> sleepQueue = new Arraylist<>();

    //Changed to linkedlist because better popping runtime complexity
    private LinkedList<KThread> sleepQueue = new LinkedList();
}
