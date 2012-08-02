package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.QueueStatus;

import java.util.HashMap;
import java.util.Map;

public class WorkerQueueStatusTask extends LoggedRecursiveTask<QueueStatus> {

    private final Events events;

    public WorkerQueueStatusTask(Events events) {
        super("Inferring queue stats");
        this.events = events;
    }

    @Override
    public QueueStatus doWork() throws Exception {

        QueueStatus status = new QueueStatus(events.getWorkers());

        Map<Integer, Long> taskToWorker = new HashMap<>();

        for (Event e : events) {
            switch (e.eventType) {
                case FORK:
                    status.add(e.time, e.workerId);
                    taskToWorker.put(e.taskHC, e.workerId);
                    break;

                case EXEC:
                    Long owner = taskToWorker.remove(e.taskHC);

                    if (owner == null) {
                        // assume external
                        break;
                    }

                    status.remove(e.time, owner);
                    break;
            }
        }

        System.err.println("maxDepth = " + status.getMaxCount());
        return status;
    }
}
