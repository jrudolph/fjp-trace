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
                    taskToWorker.put(e.taskHC, SUBMISSION_WORKER);
                    break;

                case FORK:
                    status.register(e.time, e.workerId, currentCount.add(e.workerId));
                    taskToWorker.put(e.taskHC, e.workerId);
                    break;

                case EXEC: {
                    Long owner = taskToWorker.remove(e.taskHC);

                    if (owner == null) {
                        System.err.println("WARNING: No owner is recorded for executing task! This event: " + e);
                        break;
                    }

                    if (owner == SUBMISSION_WORKER) {
                        break;
                    }

                    status.register(e.time, owner, currentCount.add(owner, -1));
                    break;
                }

                case JOINED: {
                    Long owner = taskToWorker.remove(e.taskHC);

                    if (owner != null) {
                        System.err.println("WARNING: Joined the task without prior record of execution, assume it had executed, fixing up the queue. This event: " + e);
                        status.register(e.time, owner, currentCount.add(owner, -1));
                    }

                    break;
                }

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
