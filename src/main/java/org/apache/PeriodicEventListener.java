package org.apache;

public interface PeriodicEventListener {

    /**
     * Execute a periodic task, such as reloading, etc.
     */
    public void periodicEvent();
}
