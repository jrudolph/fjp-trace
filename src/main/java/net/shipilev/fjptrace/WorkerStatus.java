package net.shipilev.fjptrace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class WorkerStatus {

    private final Map<Long,Timeline<WorkerStatusBL>> blTimelines = new HashMap<>();
    private final Map<Long,Timeline<WorkerStatusPK>> pkTimelines = new HashMap<>();
    private final Map<Long,Timeline<WorkerStatusJN>> jnTimelines = new HashMap<>();

    private final Set<Long> workers = new HashSet<>();
    private final SortedSet<Long> times = new TreeSet<>();

    public void add(long time, long worker, WorkerStatusBL status) {
        ensureWorker(worker);
        times.add(time);
        blTimelines.get(worker).add(time, status);
    }

    public void add(long time, long worker, WorkerStatusPK status) {
        ensureWorker(worker);
        times.add(time);
        pkTimelines.get(worker).add(time, status);
    }

    public void add(long time, long worker, WorkerStatusJN status) {
        ensureWorker(worker);
        times.add(time);
        jnTimelines.get(worker).add(time, status);
    }

    private void ensureWorker(long worker) {
        if (workers.add(worker)) {
            Timeline<WorkerStatusBL> vBL = new Timeline<>();
            Timeline<WorkerStatusPK> vPK = new Timeline<>();
            Timeline<WorkerStatusJN> vJN = new Timeline<>();
            vBL.add(-1, WorkerStatusBL.IDLE);
            vPK.add(-1, WorkerStatusPK.PARKED);
            vJN.add(-1, WorkerStatusJN.FREE);
            blTimelines.put(worker, vBL);
            pkTimelines.put(worker, vPK);
            jnTimelines.put(worker,  vJN);
        }

    }

    public SortedSet<Long> getTimes() {
        return times;
    }

    public WorkerStatusBL getBLStatus(long worker, long time) {
        return blTimelines.get(worker).getStatus(time);
    }

    public WorkerStatusPK getPKStatus(long worker, long time) {
        return pkTimelines.get(worker).getStatus(time);
    }

    public WorkerStatusJN getJNStatus(long worker, long time) {
        return jnTimelines.get(worker).getStatus(time);
    }
}