package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;

import java.util.HashMap;
import java.util.Map;

public class CheckEventsTask extends LoggedRecursiveAction {

    private final Events events;

    public CheckEventsTask(Events events) {
        super("Checking events");
        this.events = events;
    }

    @Override
    public void doWork() throws Exception {
        Map<Integer, Event> executingTasks = new HashMap<>();

        for (Event e : events) {

            switch (e.eventType) {
                case EXEC: {
                    Event prev = executingTasks.put(e.taskHC, e);
                    if (prev != null) {
                        System.err.println("WARNING: Already executing the task! This event: " + e.toString() + ", but executed by " + prev.toString());
                    }
                    break;
                }

                case EXECED: {
                    Event prev = executingTasks.remove(e.taskHC);
                    if (prev == null) {
                        System.err.println("WARNING: Finishing not yet started task! This event: " + e.toString());
                    }
                    break;
                }

            }

        }
    }
}
