package net.shipilev.fjptrace;

/**
 * Join/helping status
 */
public enum WorkerStatusJN {
    /**
     * Not joining any other tasks
     */
    FREE,

    /**
     * Helping to execute other tasks
     */
    JOINING,
}
