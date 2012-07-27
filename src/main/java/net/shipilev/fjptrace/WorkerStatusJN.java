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

    /**
     * The status is unknown
     */
    UNKNOWN,

    ;

    static WorkerStatusJN deNull(WorkerStatusJN in) {
        return (in == null) ? UNKNOWN : in;
    }
}
