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
