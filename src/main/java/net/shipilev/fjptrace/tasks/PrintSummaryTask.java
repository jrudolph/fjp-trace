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
import net.shipilev.fjptrace.util.Multiset;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class PrintSummaryTask extends LoggedRecursiveAction {

    private final TaskStatus subgraphs;
    private final String fileName;
    private final Events events;

    public PrintSummaryTask(Options opts, Events events, TaskStatus subgraphs) {
        super("Print summary");
        this.events = events;
        this.fileName = opts.getTargetPrefix() + "-summary.txt";
        this.subgraphs = subgraphs;
    }

    @Override
    public void doWork() throws Exception {
        // walk the trees

        PrintWriter pw = new PrintWriter(fileName);

        LayerStatistics global = new LayerStatistics();
        SortedMap<Integer, LayerStatistics> layerStats = new TreeMap<>();

        for (Task t : subgraphs.getParents()) {

            // compute transitive closure

            Set<Task> visited = new HashSet<>();
            Set<Long> workers = new HashSet<>();
            List<Task> prev = new ArrayList<>();
            List<Task> cur = new ArrayList<>();

            int depth = 0;

            cur.add(t);
            while (visited.addAll(cur)) {
                prev.clear();
                prev.addAll(cur);
                cur.clear();

                Set<Long> layerWorkers = new HashSet<>();

                LayerStatistics layerStat = layerStats.get(depth);
                if (layerStat == null) {
                    layerStat = new LayerStatistics();
                    layerStats.put(depth, layerStat);
                }

                for (Task c : prev) {
                    Collection<Task> children = c.getChildren();
                    if (!children.isEmpty()) {
                        cur.addAll(children);
                    }
                    layerStat.arities.addValue(children.size());
                    layerStat.selfTime.addValue(c.getSelfTime() / 1_000_000.0);
                    layerStat.totalTime.addValue(c.getTotalTime() / 1_000_000.0);

                    global.selfTime.addValue(c.getSelfTime() / 1_000_000.0);
                    global.totalTime.addValue(c.getTotalTime() / 1_000_000.0);
                    global.arities.addValue(children.size());

                    layerWorkers.add(c.getWorker());
                }

                layerStat.counts.addValue(prev.size());
                layerStat.threads.addValue(layerWorkers.size());
                workers.addAll(layerWorkers);

                depth++;
            }

            global.depths.addValue(depth);
            global.counts.addValue(visited.size());
            global.threads.addValue(workers.size());
        }

        pw.println("Summary statistics:");
        pw.printf("  total external tasks = %.0f\n", layerStats.get(0).counts.getSum());
        pw.printf("  total subtasks = %.0f\n", global.counts.getSum());
        pw.printf("  total threads = %.0f\n", global.threads.getMean());

        pw.println();

        pw.println("Per task statistics:");
        pw.printf("  tasks:   sum = %10.0f, min = %5.2f, avg = %5.2f, max = %5.2f\n", global.counts.getSum(), global.counts.getMin(), global.counts.getMean(), global.counts.getMax());
        pw.printf("  depth:                     min = %5.2f, avg = %5.2f, max = %5.2f\n", global.depths.getMin(), global.depths.getMean(), global.depths.getMax());
        pw.printf("  arity:                     min = %5.2f, avg = %5.2f, max = %5.2f\n", global.arities.getMin(), global.arities.getMean(), global.arities.getMax());
        pw.printf("  threads:                   min = %5.2f, avg = %5.2f, max = %5.2f\n", global.threads.getMin(), global.threads.getMean(), global.threads.getMax());

        pw.println();
        pw.println("Per task + per depth statistics:");
        for (Integer depth : layerStats.keySet()) {
            LayerStatistics s = layerStats.get(depth);
            pw.printf("  Depth = %d: \n", depth);
            pw.printf("    tasks:           sum = %10.0f, min = %5.2f, avg = %5.2f, max = %5.2f\n", s.counts.getSum(), s.counts.getMin(), s.counts.getMean(), s.counts.getMax());
            pw.printf("    self time (ms):  sum = %10.0f, min = %5.2f, avg = %5.2f, max = %5.2f\n", s.selfTime.getSum(), s.selfTime.getMin(), s.selfTime.getMean(), s.selfTime.getMax());
            pw.printf("    total time (ms): sum = %10.0f, min = %5.2f, avg = %5.2f, max = %5.2f\n", s.totalTime.getSum(), s.totalTime.getMin(), s.totalTime.getMean(), s.totalTime.getMax());
            pw.printf("    arity:                             min = %5.2f, avg = %5.2f, max = %5.2f\n", s.arities.getMin(), s.arities.getMean(), s.arities.getMax());
            pw.printf("    threads:                           min = %5.2f, avg = %5.2f, max = %5.2f\n", s.threads.getMin(), s.threads.getMean(), s.threads.getMax());
        }

        summarizeEvents(pw, events);

        pw.flush();
        pw.close();
    }

    private void summarizeEvents(PrintWriter pw, Events events) {
        SummaryStatistics completeTimes = new SummaryStatistics();
        SummaryStatistics execTimes = new SummaryStatistics();
        Map<Integer, Long> times = new HashMap<>();

        for (Event e : events) {
            switch (e.eventType) {
                case COMPLETING:
                    times.put(e.tag, e.time);
                    break;
                case COMPLETED: {
                    Long startTime = times.get(e.tag);
                    if (startTime != null) {
                        completeTimes.addValue(e.time - startTime);
                    }
                    break;
                }
                case EXEC:
                    times.put(e.tag, e.time);
                    break;
                case EXECUTED:
                    Long startTime = times.get(e.tag);
                    if (startTime != null) {
                        execTimes.addValue(e.time - startTime);
                    }
                    break;
            }
        }

        pw.println();
        pw.println("EXEC -> EXECUTED: " + TimeUnit.NANOSECONDS.toMillis((long) execTimes.getSum()) + "ms");
        pw.println("COMPLETING -> COMPLETED: " + TimeUnit.NANOSECONDS.toMillis((long) completeTimes.getSum()) + "ms");

    }

    private static class LayerStatistics {

        final DescriptiveStatistics depths = new DescriptiveStatistics();
        final DescriptiveStatistics arities = new DescriptiveStatistics();
        final DescriptiveStatistics counts = new DescriptiveStatistics();
        final DescriptiveStatistics threads = new DescriptiveStatistics();
        final DescriptiveStatistics selfTime = new DescriptiveStatistics();
        final DescriptiveStatistics totalTime = new DescriptiveStatistics();

    }

}
