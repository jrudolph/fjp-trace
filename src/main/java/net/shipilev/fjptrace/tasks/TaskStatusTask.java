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
import net.shipilev.fjptrace.Task;
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
        TaskStatus taskStatus = new TaskStatus(events.getWorkers());

        Map<Task, Long> execTime = new HashMap<>();
        Map<Task, Long> lastSelfTime = new HashMap<>();

        Map<Long, Task> currentExec = new HashMap<>();
        Map<Task, Task> parentTasks = new HashMap<>();

        Multiset<Task> timings = new Multiset<>();

        Map<Task, Integer> taskToID = new HashMap<>();

        int externalTaskID = 0;

        for (Event e : events) {
            switch (e.eventType) {
                case SUBMIT: {
                    Task task = taskStatus.newTask(e.taskTag);
                    taskToID.put(task, externalTaskID++);
                    taskStatus.parent(task);
                    break;
                }

                case FORK: {
                    Task task = taskStatus.newTask(e.taskTag);
                    Task currentTask = currentExec.get(e.workerId);
                    taskToID.put(task, taskToID.get(currentTask));
                    taskStatus.link(currentTask, task);
                    break;
                }

                case EXEC: {
                    Task currentTask = currentExec.get(e.workerId);
                    Task newTask = taskStatus.get(e.taskTag);

                    if (currentTask != null) {
                        // about to leave parent

                        parentTasks.put(newTask, currentTask);

                        Long start = lastSelfTime.remove(currentTask);
                        if (start == null) {
                            continue;
                        }
                        timings.add(currentTask, e.time - start);
                    }

                    // start executing
                    lastSelfTime.put(newTask, e.time);
                    currentExec.put(e.workerId, newTask);
                    execTime.put(newTask, e.time);

                    Integer id = taskToID.get(currentTask);
                    if (id != null) {
                        taskToID.put(newTask, id);
                    }

                    Integer thisTaskId = taskToID.get(newTask);
                    if (thisTaskId != null) {
                        taskStatus.register(e.time, e.workerId, thisTaskId);
                    }

                    break;
                }

                case EXECUTED: {
                    // record worker is free
                    Task task = currentExec.remove(e.workerId);

                    // count remaining self time
                    Long s = lastSelfTime.remove(task);
                    if (s == null) {
                        continue;
                    }
                    timings.add(task, e.time - s);
                    taskStatus.addSelf((e.time - timings.count(task) / 2), timings.count(task));
                    timings.removeKey(task);

                    // count the time
                    Long s1 = execTime.remove(task);
                    if (s1 == null) {
                        continue;
                    }
                    taskStatus.addTotal((e.time + s1) / 2, e.time - s1);

                    Task parent = parentTasks.remove(task);
                    if (parent != null) {
                        // getting back to parent
                        lastSelfTime.put(parent, e.time);
                        currentExec.put(e.workerId, parent);

                        // next task is parent
                        Integer parentId = taskToID.get(parent);
                        if (parentId != null) {
                            taskStatus.register(e.time, e.workerId, parentId);
                        }
                    } else {
                        // this is parent, no other tasks
                        taskStatus.register(e.time, e.workerId, TaskStatus.NO_ID);
                    }

                    break;
                }
            }
        }

        return taskStatus;
    }
}
