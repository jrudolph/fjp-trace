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
        Map<Integer, Event> forkedTasks = new HashMap<>();
        Map<Integer, Event> executingTasks = new HashMap<>();

        for (Event e : events) {

            switch (e.eventType) {
                case SUBMIT: {
                    Event prev = submittedTasks.put(e.taskHC, e);
                    if (prev != null) {
                        System.err.println("WARNING: Submitting the same task twice! This event: " + e + ", other event was " + prev);
                    }

                    break;
                }

                case FORK: {
                    if (executingTasks.containsKey(e.taskHC)) {
                        System.err.println("WARNING: Forking already executing task! This event: " + e);
                    }

                    Event prev = forkedTasks.put(e.taskHC, e);
                    if (prev != null) {
                        System.err.println("WARNING: Forking the same task twice! This event: " + e + ", other event was " + prev);
                    }

                    break;
                }

                case EXEC: {
                    Event prev = executingTasks.put(e.taskHC, e);
                    if (prev != null) {
                        System.err.println("WARNING: Already executing the task! This event: " + e + ", but executed by " + prev);
                    }

                    Event forkedEvent = forkedTasks.remove(e.taskHC);
                    Event submitEvent = submittedTasks.remove(e.taskHC);
                    if (forkedEvent == null && submitEvent == null) {
                        System.err.println("WARNING: Executing the task of unknown origin! This event: " + e);
                    }

                    break;
                }

                case EXECED: {
                    Event prev = executingTasks.remove(e.taskHC);
                    if (prev == null) {
                        System.err.println("WARNING: Finishing not yet started task! This event: " + e);
                    }
                    break;
                }

            }

        }
    }
}
