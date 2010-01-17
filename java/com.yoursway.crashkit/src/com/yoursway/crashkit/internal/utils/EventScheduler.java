package com.yoursway.crashkit.internal.utils;

import com.yoursway.crashkit.internal.Constants;

/**
 * Schedules a single event in “X milliseconds from now” style.
 * 
 * <p>
 * At any given moment the event is either not scheduled or scheduled to occur
 * once. Several requests to schedule the event result in the event firing once
 * at the most distant time (out of all the requested times).
 * </p>
 * 
 * <p>
 * This class is safe for use from multiple threads.
 * </p>
 */
public class EventScheduler {
    
    private static final long RIGHT_NOW = -1;
    
    private static final long NOT_SCHEDULED = 0;
    
    private long scheduledAt;
    
    /**
     * After the return of this method (be it because of timeout or because the
     * scheduled time has come), the event becomes non-scheduled.
     */
    public synchronized void waitForScheduledEvent(long timeout) throws InterruptedException {
        long now = System.currentTimeMillis();
        long bailOut = (timeout == 0 ? Long.MAX_VALUE : now + timeout);
        long timeoutThisTime = minTimeout(timeout(now), bailOut - now);
        while (timeoutThisTime != RIGHT_NOW) {
            if (now >= bailOut)
                break;
            wait(timeoutThisTime);
            now = System.currentTimeMillis();
            timeoutThisTime = minTimeout(timeout(now), bailOut - now);
        }
        if (Constants.DEBUG_SCHEDULING)
            System.out.println("Event (or timeout) happened.");
        scheduledAt = NOT_SCHEDULED; // not really necessary, just for debugging/clarity
    }
    
    private static long minTimeout(long a, long b) {
        if (a == NOT_SCHEDULED)
            return b;
        if (b == NOT_SCHEDULED)
            return a;
        return Math.min(a, b);
    }
    
    /**
     * The return value of this method is designed to be fed into
     * <code>wait(timeout)</code> (after checking for <code>-1</code>).
     * 
     * @return <code>0</code> if the event has not been scheduled,
     *         <code>-1</code> if the event is due (or past due), and the number
     *         of milliseconds to wait otherwise
     */
    private long timeout(long now) {
        if (scheduledAt == NOT_SCHEDULED)
            return NOT_SCHEDULED;
        if (now < scheduledAt)
            return scheduledAt - now;
        return RIGHT_NOW;
    }
    
    /**
     * Schedules the event <code>timeout</code> milliseconds from now.
     * 
     * <p>
     * If the event has already been scheduled to occur earlier, reschedules the
     * event to occur in the specified time. If the went has already been
     * scheduled to occur later that the specified time, does nothing.
     * </p>
     */
    public synchronized void scheduleIn(long timeout) {
        long now = System.currentTimeMillis();
        if (scheduledAt == NOT_SCHEDULED)
            scheduledAt = now + timeout;
        else
            scheduledAt = Math.max(scheduledAt, now + timeout);
        if (Constants.DEBUG_SCHEDULING)
            System.out.println("Event scheduled.");
        notifyAll();
    }
}
