/*
 * Copyright (c) 2012 Aleksey Shipilev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shipilev.fjptrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Events implements Iterable<Event> {
    private final List<Event> events;
    private final SortedSet<Long> workers = new TreeSet<>();
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

        // assert all events are populated
        for (int c = 0; c < events.size(); c++) {
            Event e = events.get(c);
            assert e != null : "events[" + c + "] is null";

        }

        // filter out submitters
        Set<Long> onlySubmissionWorkers = new HashSet<>(workers);
        for (Event e : events) {
            if (e.eventType != EventType.SUBMIT) {
                onlySubmissionWorkers.remove(e.workerId);
            }
        }
        workers.removeAll(onlySubmissionWorkers);

        // cut off when some thread has no more events (assume we miss something beyond)
        Map<Long, Long> lastTime = new HashMap<>();
        for (Event e : events) {
            lastTime.put(e.workerId, e.time);
        }

        long cutoff = Long.MAX_VALUE;
        for (Long time : lastTime.values()) {
            cutoff = Math.min(cutoff, time);
        }

        List<Event> newEvents = new ArrayList<>();
        for (Event e : events) {
            if (e.time <= cutoff) {
                newEvents.add(e);
            }
        }

        System.out.println(events.size() + " events read");

        events.clear();
        events.addAll(newEvents);

        System.out.println(events.size() + " events after cutoff");

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
