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
import net.shipilev.fjptrace.TaskSubgraphs;

import java.util.HashMap;
import java.util.Map;

public class TaskSubgraphTask extends LoggedRecursiveTask<TaskSubgraphs> {

    private final Events events;

    public TaskSubgraphTask(Events events) {
        super("Looking for tasks topology");
        this.events = events;
    }

    @Override
    public TaskSubgraphs doWork() throws Exception {
        TaskSubgraphs subgraphs = new TaskSubgraphs(events.getWorkers());

        Map<Long, Integer> currentExec = new HashMap<>();
        Map<Integer, Integer> parentTasks = new HashMap<>();

        Map<Integer, Integer> taskToID = new HashMap<>();

        int externalTaskID = 0;

        for (Event e : events) {
            switch (e.eventType) {
                case SUBMIT:
                    taskToID.put(e.taskTag, externalTaskID++);
                    subgraphs.parent(e.taskTag);
                    break;

                case FORK: {
                    Integer currentTask = currentExec.get(e.workerId);
                    taskToID.put(e.taskTag, taskToID.get(currentTask));
                    subgraphs.link(currentTask, e.taskTag);
                    break;
                }

                case EXEC:
                    Integer currentTask = currentExec.get(e.workerId);
                    if (currentTask != null) {
                        parentTasks.put(e.taskTag, currentTask);
                    }

                    Integer id = taskToID.get(currentTask);
                    if (id != null) {
                        taskToID.put(e.taskTag, id);
                    }

                    Integer thisTaskId = taskToID.get(e.taskTag);
                    if (thisTaskId != null) {
                        subgraphs.register(e.time, e.workerId, thisTaskId);
                    }

                    currentExec.put(e.workerId, e.taskTag);

                    break;

                case EXECUTED:
                    // record worker is free
                    currentExec.remove(e.workerId);

                    Integer parent = parentTasks.remove(e.taskTag);
                    if (parent != null) {
                        // getting back to parent
                        currentExec.put(e.workerId, parent);

                        // next task is parent
                        Integer parentId = taskToID.get(parent);
                        if (parentId != null) {
                            subgraphs.register(e.time, e.workerId, parentId);
                        }
                    } else {
                        // this is parent, no other tasks
                        subgraphs.register(e.time, e.workerId, TaskSubgraphs.NO_ID);
                    }

                    break;
            }
        }

        return subgraphs;
    }
}
