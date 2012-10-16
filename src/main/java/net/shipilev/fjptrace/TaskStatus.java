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

import net.shipilev.fjptrace.util.PairedList;
import net.shipilev.fjptrace.util.Timeline;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class TaskStatus {

    public Map<Integer, PairedList> getSelf() {
        SortedSet<Task> sortedSet = new TreeSet<>(new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return Long.compare(o1.getTime(), o2.getTime());
            }
        });

        sortedSet.addAll(tasks.values());

        Map<Integer, PairedList> depthLists = new TreeMap<>();

        for (Task t : sortedSet) {
            PairedList pairs = depthLists.get(t.getDepth());
            if (pairs == null) {
                pairs = new PairedList();
                depthLists.put(t.getDepth(), pairs);
            }

            pairs.add(t.getTime(), t.getSelfTime());
        }

        return depthLists;
    }

    public Map<Integer, PairedList> getTotal() {
        SortedSet<Task> sortedSet = new TreeSet<>(new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return Long.compare(o1.getTime(), o2.getTime());
            }
        });

        sortedSet.addAll(tasks.values());

        Map<Integer, PairedList> depthLists = new TreeMap<>();

        for (Task t : sortedSet) {
            PairedList pairs = depthLists.get(t.getDepth());
            if (pairs == null) {
                pairs = new PairedList();
                depthLists.put(t.getDepth(), pairs);
            }

            pairs.add(t.getTime(), t.getTotalTime());
        }

        return depthLists;
    }

    private final Map<Long, Timeline<Integer>> tl;
    private final SortedSet<Long> times;
    private final SortedSet<Integer> ids;

    private final Map<Integer, Task> tasks;
    private final Set<Task> parents;
    private final Set<Task> orphans;

    public static final int NO_ID = -1;

    public TaskStatus(Collection<Long> workers) {
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

    public void parent(Task parentTask) {
        parentTask.setDepth(0);
        parents.add(parentTask);
    }

    public void link(Task parentTask, Task childTask) {
        if (parentTask == null) {
            orphans.add(childTask);
            return;
        }
        childTask.setDepth(parentTask.getDepth() + 1);
        parentTask.addChild(childTask);
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

    public Task newTask(int taskTag) {
        Task task = get(taskTag);
        if (task == null) {
            task = new Task(taskTag);
            tasks.put(taskTag, task);
        }
        return task;
    }

    public Task get(int taskTag) {
        return tasks.get(taskTag);
    }

    public void recordEvent(Event e) {
        newTask(e.tag).recordEvent(e);
    }

}
