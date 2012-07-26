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

        QueueStatus status = new QueueStatus();

        Map<Integer, Long> taskToWorker = new HashMap<>();

        for (Event e : events) {
            switch (e.eventType) {
                case FORK:
                    status.registerAdd(e.time, e.workerId);
                    taskToWorker.put(e.taskHC, e.workerId);
                    break;

                case EXEC:
                    Long owner = taskToWorker.remove(e.taskHC);

                    if (owner == null) {
                        // assume external task
                        break;
                    }

                    if (owner != e.workerId) {
                        status.registerSteal(e.time, owner);
                    } else {
                        status.registerExec(e.time, owner);
                    }
                    break;
            }
        }

        return status;
    }
}
