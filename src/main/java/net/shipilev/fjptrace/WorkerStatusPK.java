package net.shipilev.fjptrace;

public enum WorkerStatusPK {
    /**
     * Thread is parked
     */
    PARKED,

    /**
     * Thread is active
     */
    ACTIVE,

    /**
     * The status is unknown
     */
    UNKNOWN,

    ;

    static WorkerStatusPK deNull(WorkerStatusPK in) {
        return (in == null) ? UNKNOWN : in;
    }

}
