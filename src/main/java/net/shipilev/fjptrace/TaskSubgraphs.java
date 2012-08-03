package net.shipilev.fjptrace;

import net.shipilev.fjptrace.util.Timeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class TaskSubgraphs {

    private final Map<Long, Timeline<Integer>> tl;
    private final SortedSet<Long> times;
    private final SortedSet<Integer> ids;

    public static final int NO_ID = -1;

    public TaskSubgraphs(Collection<Long> workers) {
        this.times = new TreeSet<>();
        this.ids = new TreeSet<>();
        this.tl = new HashMap<>();
        for (long w : workers) {
            tl.put(w, new Timeline<Integer>());
        }
    }

    public void register(long time, long workerId, int id) {
        times.add(time);
        ids.add(id);
        tl.get(workerId).add(time, id);
    }

    public SortedSet<Long> getTimes() {
        return times;
    }

    public SortedSet<Integer> getIds() {
        return ids;
    }

    public Integer get(long tick, long worker) {
        Timeline<Integer> tl = this.tl.get(worker);
        if (tl != null) {
            return tl.getStatus(tick);
        } else {
            return null;
        }
    }
}

