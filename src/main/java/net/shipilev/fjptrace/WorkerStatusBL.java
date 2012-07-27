package net.shipilev.fjptrace;

/**
 * Business logic status
 */
public enum WorkerStatusBL {
    /**
     * Actively executing business logic tasks
     */
    RUNNING,

    /**
     * Not executing business logic tasks
     */
    IDLE,

    /**
     * The status is unknown
     */
    UNKNOWN,

    ;

    static WorkerStatusBL deNull(WorkerStatusBL in) {
        return (in == null) ? UNKNOWN : in;
    }

}
