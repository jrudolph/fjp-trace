package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.QueueStatus;
import net.shipilev.fjptrace.util.Multiset;

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
        Multiset<Long> currentCount = new Multiset<>();

        for (Event e : events) {
            switch (e.eventType) {
                case FORK:
                    status.register(e.time, e.workerId, currentCount.add(e.workerId));
                    taskToWorker.put(e.taskHC, e.workerId);
                    break;

                case EXEC:
                    Long owner = taskToWorker.remove(e.taskHC);

                    if (owner == null) {
                        // assume external
                        break;
                    }

                    status.register(e.time, owner, currentCount.add(owner, -1));
                    break;

                case PARK:
                    if (e.taskHC == 0) {
                        if (currentCount.count(e.workerId) != 0) {
                            System.err.println("WARNING: parking idle thread, but analyzer thinks it's workqueue is not empty, resetting queue");
                            currentCount.removeKey(e.workerId);
                            status.markInvalid(e.time, e.workerId);
                        }
                    }
                    break;

                case UNPARK:
                    if (e.taskHC == 0) {
                        if (currentCount.count(e.workerId) != 0) {
                            System.err.println("WARNING: unparking idle thread, but analyzer thinks it's workqueue is not empty, resetting queue");
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
