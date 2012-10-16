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

package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Options;
import net.shipilev.fjptrace.Task;
import net.shipilev.fjptrace.TaskStatus;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class PrintTaskTreesTask extends LoggedRecursiveAction {

    private final TaskStatus subgraphs;
    private final String fileName;
    private final Events events;
    private final long fromTime;
    private final long toTime;

    public PrintTaskTreesTask(Options opts, Events events, TaskStatus subgraphs) {
        super("Print task subtrees");
        this.fileName = opts.getTargetPrefix() + "-subtrees.dot";
        this.fromTime = opts.getFromTime();
        this.toTime = opts.getToTime();
        this.events = events;
        this.subgraphs = subgraphs;
    }

    @Override
    public void doWork() throws Exception {
        // walk the trees

        PrintWriter pw = new PrintWriter(fileName);

        // only record the events for the interesting region
        for (Event e : events) {
            if (e.time < fromTime) continue;
            if (e.time > toTime) break;
            subgraphs.recordEvent(e);
        }

        // only care about the parents in the interesting region
        Collection<Task> interestingParents = new ArrayList<>();
        for (Task t : subgraphs.getParents()) {
            if (fromTime < t.getTime() && t.getTime() < toTime) {
                interestingParents.add(t);
            }
        }

        for (Task t : interestingParents) {

            // compute transitive closure

            Set<Task> visited = new HashSet<>();
            List<Task> prev = new ArrayList<>();
            List<Task> cur = new ArrayList<>();

            List<Long> workers = new ArrayList<>();
            List<Event> events = new ArrayList<>();

            cur.add(t);
            while (visited.addAll(cur)) {
                prev.clear();
                prev.addAll(cur);
                cur.clear();

                for (Task c : prev) {
                    Collection<Task> children = c.getChildren();
                    if (!children.isEmpty()) {
                        cur.addAll(children);
                    }
                    events.addAll(c.getEvents());
                    workers.add(c.getWorker());
                }

            }

            // emit graph info

            Collections.sort(events, new Comparator<Event>() {
                @Override
                public int compare(Event o1, Event o2) {
                    return Long.compare(o1.time, o2.time);
                }
            });

            pw.println("digraph {");

            // emit all sequencing for workers first
            for (Long w : workers) {

                Event lastEvent = null;
                for (Event e : events) {
                    if (e.workerId == w) {
                        if (lastEvent != null) {
                            pw.println(" " + lastEvent.shortID() + " -> " + e.shortID() + " [timeDiff = " + (e.time - lastEvent.time) + "];");
                        }
                        lastEvent = e;
                    }
                }
            }

            pw.println("}");
        }

        pw.flush();
        pw.close();
    }

}
