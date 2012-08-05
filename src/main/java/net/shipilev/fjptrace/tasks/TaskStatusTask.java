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
import net.shipilev.fjptrace.TaskStatus;
import net.shipilev.fjptrace.util.Multiset;

import java.util.HashMap;
import java.util.Map;

public class TaskStatusTask extends LoggedRecursiveTask<TaskStatus> {

    private final Events events;

    public TaskStatusTask(Events events) {
        super("Computing task stats");
        this.events = events;
    }

    @Override
    public TaskStatus doWork() throws Exception {
        TaskStatus taskStatus = new TaskStatus();

        Map<Integer, Long> execTime = new HashMap<>();
        Map<Integer, Long> lastSelfTime = new HashMap<>();

        Map<Long, Integer> currentExec = new HashMap<>();
        Map<Integer, Integer> parentTasks = new HashMap<>();

        Multiset<Integer> timings = new Multiset<>();

        for (Event e : events) {
            switch (e.eventType) {
                case EXEC:
                    Integer currentTask = currentExec.get(e.workerId);

                    if (currentTask != null) {
                        // about to leave parent
                        parentTasks.put(e.taskHC, currentTask);

                        Long start = lastSelfTime.remove(currentTask);
                        if (start == null) {
                            continue;
                        }
                        timings.add(currentTask, e.time - start);
                    }

                    // start executing
                    lastSelfTime.put(e.taskHC, e.time);
                    currentExec.put(e.workerId, e.taskHC);
                    execTime.put(e.taskHC, e.time);

                    break;

                case EXECUTED:
                    // record worker is free
                    currentExec.remove(e.workerId);

                    // count remaining self time
                    Long s = lastSelfTime.remove(e.taskHC);
                    if (s == null) {
                        continue;
                    }
                    timings.add(e.taskHC, e.time - s);
                    taskStatus.addSelf((e.time - timings.count(e.taskHC) / 2), timings.count(e.taskHC));
                    timings.removeKey(e.taskHC);

                    // count the time
                    Long s1 = execTime.remove(e.taskHC);
                    if (s1 == null) {
                        continue;
                    }
                    taskStatus.addTotal((e.time + s1) / 2, e.time - s1);

                    Integer parent = parentTasks.remove(e.taskHC);
                    if (parent != null) {
                        // getting back to parent
                        lastSelfTime.put(parent, e.time);
                        currentExec.put(e.workerId, parent);
                    }

                    break;
            }
        }

        return taskStatus;
    }
}
