package net.shipilev.fjptrace;

import net.shipilev.fjptrace.util.Timeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class WorkerStatus {

    private final Map<Long, WorkerStatusHolder> currentStatus = new HashMap<>();
    private final Map<Long,Timeline<WorkerStatusHolder>> timeline = new HashMap<>();

    private final Set<Long> workers = new HashSet<>();
    private final SortedSet<Long> times = new TreeSet<>();

    public void add(long time, long worker, WorkerStatusBL status) {
        ensureWorker(worker);
        times.add(time);

        WorkerStatusHolder globalStatus = currentStatus.get(worker);
        globalStatus = globalStatus.merge(status);
        currentStatus.put(worker, globalStatus);
        timeline.get(worker).add(time, globalStatus);
    }

    public void add(long time, long worker, WorkerStatusPK status) {
        ensureWorker(worker);
        times.add(time);

        WorkerStatusHolder globalStatus = currentStatus.get(worker);
        globalStatus = globalStatus.merge(status);
        currentStatus.put(worker, globalStatus);
        timeline.get(worker).add(time, globalStatus);
    }

    public void add(long time, long worker, WorkerStatusJN status) {
        ensureWorker(worker);
        times.add(time);

        WorkerStatusHolder globalStatus = currentStatus.get(worker);
        globalStatus = globalStatus.merge(status);
        currentStatus.put(worker, globalStatus);
        timeline.get(worker).add(time, globalStatus);
    }

    public void markInvalid(long time, long worker) {
        timeline.get(worker).removeBefore(time);
    }

    private void ensureWorker(long worker) {
        if (workers.add(worker)) {
            Timeline<WorkerStatusHolder> tl = new Timeline<WorkerStatusHolder>();
            tl.add(-1, WorkerStatusHolder.DEFAULT);
            timeline.put(worker, tl);
            currentStatus.put(worker, WorkerStatusHolder.DEFAULT);
        }
    }

    public SortedSet<Long> getTimes() {
        return times;
    }

    public WorkerStatusHolder getStatus(long worker, long time) {
        Timeline<WorkerStatusHolder> tl = timeline.get(worker);
        if (tl == null) {
            return WorkerStatusHolder.UNKNOWN;
        } else {
            WorkerStatusHolder status = tl.getStatus(time);
            return (status == null) ? WorkerStatusHolder.UNKNOWN : status;
        }
    }


}
