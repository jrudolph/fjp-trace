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
import net.shipilev.fjptrace.TaskSubgraphs;
import net.shipilev.fjptrace.util.GZIPOutputStreamEx;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrintSubTreesTask extends LoggedRecursiveAction {

    private final TaskSubgraphs subgraphs;

    public PrintSubTreesTask(Options opts, TaskSubgraphs subgraphs) {
        super("Print task trees");
        this.subgraphs = subgraphs;
    }

    @Override
    public void doWork() throws Exception {
        // walk the trees

        System.err.println("Parents = " + subgraphs.getParents().size());

        DescriptiveStatistics depths = new DescriptiveStatistics();
        DescriptiveStatistics arities = new DescriptiveStatistics();
        DescriptiveStatistics counts = new DescriptiveStatistics();

        for (TaskSubgraphs.Task t : subgraphs.getParents()) {

            // compute transitive closure

            Set<TaskSubgraphs.Task> visited = new HashSet<>();
            List<TaskSubgraphs.Task> prev = new ArrayList<>();
            List<TaskSubgraphs.Task> cur = new ArrayList<>();

            int depth = 0;

            cur.add(t);
            while (visited.addAll(cur)) {
                prev.clear();
                prev.addAll(cur);
                cur.clear();
                for (TaskSubgraphs.Task c : prev) {
                    Collection<TaskSubgraphs.Task> children = c.getChildren();
                    if (!children.isEmpty()) {
                        arities.addValue(children.size());
                        cur.addAll(children);
                    }
                }
                depth++;
            }

            depths.addValue(depth);
            counts.addValue(visited.size());
        }

//        getPw().println("Depths: " + depths);
//        getPw().println("Counts: " + counts);
//        getPw().println("Arities: " + arities);

    }

}
