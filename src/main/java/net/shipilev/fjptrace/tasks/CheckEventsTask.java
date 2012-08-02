package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.TaskStatus;
import net.shipilev.fjptrace.util.Multiset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckEventsTask extends LoggedRecursiveAction {

    private final Events events;

    public CheckEventsTask(Events events) {
        super("Checking events");
        this.events = events;
    }

    @Override
    public void doWork() throws Exception {
        Set<Integer> executingTasks = new HashSet<>();

        for (Event e : events) {

            switch (e.eventType) {
                case EXEC: {
                    boolean ok = executingTasks.add(e.taskHC);
                    if (!ok) {
                        System.err.println("Already executing the task: " + e.time + " " + e.toString());
                    }
                    break;
                }

                case EXECED: {
                    boolean ok = executingTasks.remove(e.taskHC);
                    if (!ok) {
                        System.err.println("Not executing the task: " + e.toString());
                    }
                    break;
                }

            }

        }
    }
}
