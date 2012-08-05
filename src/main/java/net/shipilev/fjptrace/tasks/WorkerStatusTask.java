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
import net.shipilev.fjptrace.WorkerStatus;
import net.shipilev.fjptrace.WorkerStatusBL;
import net.shipilev.fjptrace.WorkerStatusJN;
import net.shipilev.fjptrace.WorkerStatusPK;

public class WorkerStatusTask extends LoggedRecursiveTask<WorkerStatus> {

    private final Events events;

    public WorkerStatusTask(Events events) {
        super("Computing worker status");
        this.events = events;
    }

    @Override
    public WorkerStatus doWork() {
        WorkerStatus workerStatus = new WorkerStatus();

        for (long w : events.getWorkers()) {

            int execDepth = 0;
            int jnDepth = 0;

            for (Event e : events) {
                if (w != e.workerId) {
                    continue;
                }

                switch (e.eventType) {
                    case EXEC:
                        execDepth++;
                        workerStatus.add(e.time, w, WorkerStatusBL.RUNNING);
                        break;

                    case EXECED:
                        execDepth--;
                        if (execDepth == 0) {
                            workerStatus.add(e.time, w, WorkerStatusBL.IDLE);
                        }
                        break;

                    case PARK:
                        if (e.taskHC == 0) {
                            if (execDepth != 0) {
                                System.err.println("WARNING: parking idle thread, but analyzer thinks it executes the task, resetting exec depth");
                                execDepth = 0;
                                workerStatus.markInvalid(e.time, w);
                            }
                            if (jnDepth != 0) {
                                System.err.println("WARNING: parking idle thread, but analyzer thinks it joins the task, resetting join depth");
                                jnDepth = 0;
                                workerStatus.markInvalid(e.time, w);
                            }
                        }
                        workerStatus.add(e.time, w, WorkerStatusPK.PARKED);
                        break;

                    case UNPARK:
                        if (e.taskHC == 0) {
                            if (execDepth != 0) {
                                System.err.println("WARNING: parking idle thread, but analyzer thinks it executes the task, resetting exec depth");
                                execDepth = 0;
                                workerStatus.markInvalid(e.time, w);
                            }
                            if (jnDepth != 0) {
                                System.err.println("WARNING: parking idle thread, but analyzer thinks it joins the task, resetting join depth");
                                jnDepth = 0;
                                workerStatus.markInvalid(e.time, w);
                            }
                        }
                        workerStatus.add(e.time, w, WorkerStatusPK.ACTIVE);
                        break;

                    case JOIN:
                        jnDepth++;
                        workerStatus.add(e.time, w, WorkerStatusJN.JOINING);
                        break;

                    case JOINED:
                        jnDepth--;
                        if (jnDepth == 0) {
                            workerStatus.add(e.time, w, WorkerStatusJN.FREE);
                        }
                        break;
                }
            }
        }

        return workerStatus;
    }
}
