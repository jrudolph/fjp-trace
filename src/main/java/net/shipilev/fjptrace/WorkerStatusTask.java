package net.shipilev.fjptrace;

import java.util.concurrent.RecursiveTask;

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
                            assert execDepth == 0;
                            assert jnDepth == 0;
                        }
                        workerStatus.add(e.time, w, WorkerStatusPK.PARKED);
                        break;

                    case UNPARK:
                        if (e.taskHC == 0) {
                            assert execDepth == 0;
                            assert jnDepth == 0;
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