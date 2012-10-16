/*
 * Copyright (c) 2012 Aleksey Shipilev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        Map<Integer, Event> submittedTasks = new HashMap<>();
        Map<Integer, Event> invokedTasks = new HashMap<>();
        Map<Integer, Event> forkedTasks = new HashMap<>();
        Map<Integer, Event> executingTasks = new HashMap<>();

        for (Event e : events) {

            switch (e.eventType) {
                case SUBMIT: {
                    Event prev = submittedTasks.put(e.tag, e);
                    if (prev != null) {
                        getPw().println("WARNING: Submitting the same task twice! This event: " + e + ", other event was " + prev);
                    }

                    break;
                }

                case INVOKE: {
                    invokedTasks.put(e.tag, e);

                    Event forkedEvent = forkedTasks.remove(e.tag);
                    Event submitEvent = submittedTasks.remove(e.tag);
                    if (forkedEvent != null) {
                        getPw().println("WARNING: Invoking the task of which is already forked! This event: " + e);
                    }
                    if (submitEvent != null) {
                        getPw().println("WARNING: Invoking the task of which is externally submitted! This event: " + e);
                    }

                    break;
                }


                case FORK: {
                    if (executingTasks.containsKey(e.tag)) {
                        getPw().println("WARNING: Forking already executing task! This event: " + e);
                    }

                    Event prev = forkedTasks.put(e.tag, e);
                    if (prev != null) {
                        getPw().println("WARNING: Forking the same task twice! This event: " + e + ", other event was " + prev);
                    }

                    break;
                }

                case EXEC: {
                    Event prev = executingTasks.put(e.tag, e);
                    if (prev != null) {
                        getPw().println("WARNING: Already executing the task! This event: " + e + ", but executed by " + prev);
                    }

                    Event forkedEvent = forkedTasks.remove(e.tag);
                    Event submitEvent = submittedTasks.remove(e.tag);
                    Event invokedEvent = invokedTasks.remove(e.tag);
                    if (forkedEvent == null && submitEvent == null && invokedEvent == null) {
                        getPw().println("WARNING: Executing the task of unknown origin! This event: " + e);
                    }

                    break;
                }

                case EXECUTED: {
                    Event prev = executingTasks.remove(e.tag);
                    if (prev == null) {
                        getPw().println("WARNING: Finishing not yet started task! This event: " + e);
                    }
                    break;
                }

            }

        }
    }
}
