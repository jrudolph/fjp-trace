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
import net.shipilev.fjptrace.QueueStatus;
import net.shipilev.fjptrace.util.Multiset;

import java.util.HashMap;
import java.util.Map;

public class WorkerQueueStatusTask extends LoggedRecursiveTask<QueueStatus> {

    public static final long SUBMISSION_WORKER = -1;

    private final Events events;

    public WorkerQueueStatusTask(Events events) {
        super("Inferring queue stats");
        this.events = events;
    }

    @Override
    public QueueStatus doWork() throws Exception {

        QueueStatus status = new QueueStatus(events.getWorkers());

        Map<Integer, Long> taskToWorker = new HashMap<>();
        Multiset<Long> currentCount = new Multiset<>();

        for (Event e : events) {
            switch (e.eventType) {
                case SUBMIT:
                    taskToWorker.put(e.taskTag, SUBMISSION_WORKER);
                    break;

                case FORK:
                    status.register(e.time, e.workerId, currentCount.add(e.workerId));
                    taskToWorker.put(e.taskTag, e.workerId);
                    break;

                case INVOKE:
                    status.register(e.time, e.workerId, currentCount.add(e.workerId));
                    taskToWorker.put(e.taskTag, e.workerId);
                    break;

                case EXEC: {
                    Long owner = taskToWorker.remove(e.taskTag);

                    if (owner == null) {
                        getPw().println("WARNING: No owner is recorded for executing task! This event: " + e);
                        break;
                    }

                    if (owner == SUBMISSION_WORKER) {
                        break;
                    }

                    status.register(e.time, owner, currentCount.add(owner, -1));
                    break;
                }

                case JOINED: {
                    Long owner = taskToWorker.remove(e.taskTag);

                    if (owner != null) {
                        getPw().println("WARNING: Joined the task without prior record of execution, assume it had executed, fixing up the queue. This event: " + e);
                        status.register(e.time, owner, currentCount.add(owner, -1));
                    }

                    break;
                }

                case PARK:
                    if (e.taskTag <= 0) {
                        if (currentCount.count(e.workerId) != 0) {
                            getPw().println("WARNING: parking idle thread, but analyzer thinks it's workqueue is not empty, resetting queue");
                            currentCount.removeKey(e.workerId);
                            status.markInvalid(e.time, e.workerId);
                        }
                    }
                    break;

                case UNPARK:
                    if (e.taskTag <= 0) {
                        if (currentCount.count(e.workerId) != 0) {
                            getPw().println("WARNING: unparking idle thread, but analyzer thinks it's workqueue is not empty, resetting queue");
                            currentCount.removeKey(e.workerId);
                            status.markInvalid(e.time, e.workerId);
                        }
                    }
                    break;
            }
        }

        return status;
    }
}
