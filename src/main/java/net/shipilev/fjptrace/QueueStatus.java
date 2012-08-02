package net.shipilev.fjptrace;

import net.shipilev.fjptrace.util.Multiset;
import net.shipilev.fjptrace.util.Timeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class QueueStatus {

    private final Multiset<Long> currentCount = new Multiset<>();
    private final Map<Long, Timeline<Long>> workerTaskCounts = new HashMap<>();
    private final SortedSet<Long> times = new TreeSet<>();
    private long maxCount = 0;

    public QueueStatus(Collection<Long> workers) {
        for (Long w : workers) {
            workerTaskCounts.put(w, new Timeline<Long>());
        }
    }

    public void register(long time, long worker, long count) {
        times.add(time);
        workerTaskCounts.get(worker).add(time, count);
        maxCount = Math.max(count, maxCount);
    }

    public SortedSet<Long> getTimes() {
        return times;
    }

    public long getMaxCount() {
        return maxCount;
    }

    public long getDepth(long time, long w) {
        Timeline<Long> timeline = workerTaskCounts.get(w);
        if (timeline != null) {
            Long status = timeline.getStatus(time);
            if (status != null) {
                return status;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public void markInvalid(long time, long workerId) {
        workerTaskCounts.get(workerId).removeBefore(time);
    }
}
