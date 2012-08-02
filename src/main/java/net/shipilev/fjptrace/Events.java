package net.shipilev.fjptrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Events implements Iterable<Event> {
    private final List<Event> events;
    private final SortedSet<Long> workers = new TreeSet<>();
    private int count;
    private long start;
    private long end;

    public Events(int count) {
        events = new ArrayList<>(count);
        for (int c = 0; c < count; c++) {
            events.add(null);
        }
    }

    public void set(int index, Event event) {
        events.set(index, event);
    }

    public boolean add(Event event) {
        return events.add(event);
    }

    public void seal() {
        if (events.isEmpty()) {
            System.out.println("No events in the log");
            throw new IllegalStateException("No events in the log");
        }

        System.out.println(events.size() + " events read");

        // assert all events are populated
        for (int c = 0; c < events.size(); c++) {
            Event e = events.get(c);
            assert e != null : "events[" + c + "] is null";

        }

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

    public void addworker(long threadID) {
        workers.add(threadID);
    }
}
