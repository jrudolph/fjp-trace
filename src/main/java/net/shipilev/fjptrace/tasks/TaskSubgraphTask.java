package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.TaskSubgraphs;

import java.util.HashMap;
import java.util.Map;

public class TaskSubgraphTask extends LoggedRecursiveTask<TaskSubgraphs> {

    private final Events events;

    public TaskSubgraphTask(Events events) {
        super("Looking for tasks topology");
        this.events = events;
    }

    @Override
    public TaskSubgraphs doWork() throws Exception {
        TaskSubgraphs subgraphs = new TaskSubgraphs(events.getWorkers());

        Map<Long, Integer> currentExec = new HashMap<>();
        Map<Integer, Integer> parentTasks = new HashMap<>();

        Map<Integer, Integer> taskToID = new HashMap<>();

        int externalTaskID = 0;

        for (Event e : events) {
            switch (e.eventType) {
                case SUBMIT:
                    taskToID.put(e.taskHC, externalTaskID++);
                    break;

                case FORK: {
                    Integer currentTask = currentExec.get(e.workerId);
                    taskToID.put(e.taskHC, taskToID.get(currentTask));
                    break;
                }

                case EXEC:
                    Integer currentTask = currentExec.get(e.workerId);
                    if (currentTask != null) {
                        parentTasks.put(e.taskHC, currentTask);
                    }

                    Integer id = taskToID.get(currentTask);
                    if (id != null) {
                        taskToID.put(e.taskHC, id);
                    }

                    Integer thisTaskId = taskToID.get(e.taskHC);
                    if (thisTaskId != null) {
                        subgraphs.register(e.time, e.workerId, thisTaskId);
                    }

                    currentExec.put(e.workerId, e.taskHC);

                    break;

                case EXECED:
                    // record worker is free
                    currentExec.remove(e.workerId);

                    Integer parent = parentTasks.remove(e.taskHC);
                    if (parent != null) {
                        // getting back to parent
                        currentExec.put(e.workerId, parent);

                        // next task is parent
                        Integer parentId = taskToID.get(parent);
                        if (parentId != null) {
                            subgraphs.register(e.time, e.workerId, parentId);
                        }
                    } else {
                        // this is parent, no other tasks
                        subgraphs.register(e.time, e.workerId, TaskSubgraphs.NO_ID);
                    }

                    break;
            }
        }

        return subgraphs;
    }
}
