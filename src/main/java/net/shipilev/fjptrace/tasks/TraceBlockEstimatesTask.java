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
import java.util.concurrent.TimeUnit;

public class TraceBlockEstimatesTask extends LoggedRecursiveAction {

    private final Events events;

    public TraceBlockEstimatesTask(Events events) {
        super("Checking tracing overheads");
        this.events = events;
    }

    @Override
    public void doWork() throws Exception {

        Map<Long, Long> blocks = new HashMap<>();

        Long blockedSince = null;

        int maxContenders = 0;
        long totalDuration = 0;
        long linearDuration = 0;

        for (Event e : events) {

            switch (e.eventType) {
                case TRACE_BLOCK: {
                    maxContenders = Math.max(blocks.size(), maxContenders);

                    blocks.put(e.workerId, e.time);

                    if (blockedSince == null) {
                        blockedSince = e.time;
                    }

                    break;
                }

                case TRACE_UNBLOCK: {
                    long time = blocks.remove(e.workerId);
                    totalDuration += (e.time - time);

                    if (blocks.size() == 0) {
                        if (blockedSince != null) {
                            linearDuration += (e.time - blockedSince);
                        }
                        blockedSince = null;
                    }

                    break;
                }

            }

        }

        getPw().printf("Trace block duration: %d ms total, %d ms linear, %.2fx overlap, %d max contenders\n",
                TimeUnit.NANOSECONDS.toMillis(totalDuration),
                TimeUnit.NANOSECONDS.toMillis(linearDuration),
                totalDuration * 1.0 / linearDuration,
                maxContenders);

    }
}
