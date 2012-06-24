package net.shipilev.fjptrace;

import java.util.concurrent.RecursiveTask;

public class WorkerStatusTask extends RecursiveTask<WorkerStatus> {

    private final Events events;

    public WorkerStatusTask(Events events) {
        this.events = events;
    }

    @Override
    public WorkerStatus compute() {
        try {
            return call();
        } catch (Exception e) {
            return null;
        }
    }

    public WorkerStatus call() throws Exception {
        System.out.println("Computing worker status");

        WorkerStatus workerStatus = new WorkerStatus();

        int execDepth = 0;
        int jnDepth = 0;

        for (long w : events.getWorkers()) {

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
                        workerStatus.add(e.time, w, WorkerStatusPK.PARKED);
                        break;

                    case UNPARK:
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
