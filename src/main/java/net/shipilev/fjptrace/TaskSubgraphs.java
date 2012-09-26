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

import net.shipilev.fjptrace.util.Timeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TaskSubgraphs {

    private final Map<Long, Timeline<Integer>> tl;
    private final SortedSet<Long> times;
    private final SortedSet<Integer> ids;

    private final Map<Integer, Task> tasks;
    private final Set<Task> parents;
    private final Set<Task> orphans;

    public static final int NO_ID = -1;

    public TaskSubgraphs(Collection<Long> workers) {
        this.times = new TreeSet<>();
        this.ids = new TreeSet<>();
        this.parents = new HashSet<>();
        this.orphans = new HashSet<>();
        this.tasks = new HashMap<>();
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

    public void parent(int parent) {
        Task parentTask = new Task();
        tasks.put(parent, parentTask);
        parents.add(parentTask);
    }

    public void link(int parent, int child) {
        Task childTask = tasks.get(child);
        if (childTask == null) {
            childTask = new Task();
            tasks.put(child, childTask);
        }
        Task parentTask = tasks.get(parent);
        if (parentTask == null) {
            orphans.add(childTask);
        } else {
            parentTask.addChild(childTask);
        }
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

    public Collection<Task> getParents() {
        return parents;
    }

}

