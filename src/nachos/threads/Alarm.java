package nachos.threads;
import nachos.machine.*;
import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	    long currentTime = Machine.timer().getTime();

        boolean interrupt = Machine.interrupt().disable();

        while(sleepingQueue.size() > 0 && sleepingQueue.peek().getTime() <= currentTime){
            KThread t = sleepingQueue.poll().getThread();
            t.ready();
        }
        KThread.yield();

        Machine.interrupt().restore(interrupt);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	    long wakeTime = Machine.timer().getTime() + x;
    
        KThread t = KThread.currentThread();

        boolean interrupt = Machine.interrupt().disable();

        TTime tempVariable = new TTime(t, wakeTime);
        sleepingQueue.add(tempVariable);
        t.sleep();

        Machine.interrupt().restore(interrupt);
    }

    private PriorityQueue<TTime> sleepingQueue = new PriorityQueue<>();
}
//Need to implement Comparable and override compareTo method since we plan on using this object in a priority queue
class TTime implements Comparable{
    private KThread t;
    private long wakeTime;
    public TTime(KThread t, long wakeTime){
        this.t = t;
        this.wakeTime = wakeTime;
    }
    public long getTime (){
        return wakeTime;
    }
    public KThread getThread() {
    	return t;
    }
    //Override compareTo method.
    //Only compare 2 TTime objects based on wakeTime
    public int compareTo(TTime other){
        if(wakeTime > other.wakeTime){
            return 1;
        }else if(wakeTime < other.wakeTime){
            return -1;
        }else{
            return 0;
        }
    }
    public int compareTo(Object n) {
    	return 0;
    }
    
}