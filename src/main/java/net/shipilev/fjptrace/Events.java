package net.shipilev.fjptrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Events implements Iterable<Event> {
    private final ArrayList<Event> events = new ArrayList<>();
    private final SortedSet<Long> workers = new TreeSet<>();
    private long start;
    private long end;

    public boolean add(Event event) {
        workers.add(event.workerId);
        return events.add(event);
    }

    public void seal() {
        if (events.isEmpty()) {
            System.out.println("No events in the log");
            throw new IllegalStateException("No events in the log");
        }

        System.out.println(events.size() + " events read");

        Collections.sort(events);
        events.trimToSize();

        start = events.get(0).time;
        end = events.get(events.size() - 1).time;
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public void setBasetime(long basetime) {
        for (Event event : events) {
            event.time -= basetime;
        }
    }

    public Collection<Long> getWorkers() {
        return workers;
    }

    public List<Event> getList() {
        return events;
    }
}
