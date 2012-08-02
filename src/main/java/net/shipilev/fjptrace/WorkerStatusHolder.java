package net.shipilev.fjptrace;

public class WorkerStatusHolder {

    public final WorkerStatusBL blStatus;
    public final WorkerStatusJN jnStatus;
    public final WorkerStatusPK pkStatus;

    public static final WorkerStatusHolder DEFAULT = new WorkerStatusHolder(WorkerStatusBL.IDLE, WorkerStatusJN.FREE, WorkerStatusPK.PARKED);
    public static final WorkerStatusHolder UNKNOWN = new WorkerStatusHolder(WorkerStatusBL.UNKNOWN, WorkerStatusJN.UNKNOWN, WorkerStatusPK.UNKNOWN);

    public WorkerStatusHolder(WorkerStatusBL blStatus, WorkerStatusJN jnStatus, WorkerStatusPK pkStatus) {
        this.blStatus = blStatus;
        this.jnStatus = jnStatus;
        this.pkStatus = pkStatus;
    }

    public WorkerStatusHolder merge(WorkerStatusBL newBlStatus) {
        return new WorkerStatusHolder(newBlStatus, jnStatus, pkStatus);
    }

    public WorkerStatusHolder merge(WorkerStatusJN newJnStatus) {
        return new WorkerStatusHolder(blStatus, newJnStatus, pkStatus);
    }

    public WorkerStatusHolder merge(WorkerStatusPK newPkStatus) {
        return new WorkerStatusHolder(blStatus, jnStatus, newPkStatus);
    }

}
