package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    
    //Global variables
    private Lock lock;
    private Condition2 listenerWaitQueue;
    private Condition2 speakerWaitQueue;
    //private Condition2 listenerReceiveQueue;
    private Condition2 speakerSpeaking;

    private int listenerNotPaired;
    private int speakerNotSpeak;

    private boolean listenerWaiting;
    private boolean speakerWaiting;

    private boolean messageRecieved;
    private int temp;

    public Communicator() {
        //Initialize Lock
        lock = new Lock();

        //Initialize Condition Variables
        listenerWaitQueue = new Condition2(lock);
        speakerWaitQueue = new Condition2(lock);
        //listenerReceiveQueue = new Condition2(lock);
        speakerSpeaking = new Condition2(lock);

        listenerNotPiared = 0;
        speakerNotSpeak = 0;

        listenerWaiting = false;
        speakerWaiting = false;
        messageRecieved = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();

        ++speakerNotSpeak;

        while(listenerNotPaired == 0 || speakerWaiting){
        	speakerWaitQueue.sleep();
        }

        speakerWaiting = true;
        listenerWaitQueue.wake();
        --listenerNotPaired;
        temp = word;

        speakerSpeaking.sleep();
        // while(!(listenerWaiting && messageRecieved)){
        //     //listenerReceiveQueue.wake();
        //     speakerSpeakingQueue.sleep();
        // }

        // listenerWaiting = false;
        speakerWaiting = false;
        //messageRecieved = false;
        if(speakerNotSpeak > 0) speakerWaitQueue.wake();
        //listenerWaitQueue.wake();

        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        lock.acquire();

        ++listenerNotPiared;

        if(speakerNotSpeak > 0 && !speakerWaiting && !listenerWaiting){
            speakerWaitQueue.wake();
            --speakerNotSpeak;
            // messageRecieved = true;
            listenerWaiting = true;
        }
        // listenerWaiting = true;
        listenerWaitQueue.sleep()
        // while(!speakerWaiting){
        //     listenerReceiveQueue.sleep();
        // }

        speakerSpeaking.wake();
        // messageRecieved = true;

        lock.release();

        return temp;
    }
}
