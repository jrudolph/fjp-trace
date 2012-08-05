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

package net.shipilev.fjptrace;

import net.shipilev.fjptrace.util.Timeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class WorkerStatus {

    private final Map<Long, WorkerStatusHolder> currentStatus = new HashMap<>();
    private final Map<Long,Timeline<WorkerStatusHolder>> timeline = new HashMap<>();

    private final Set<Long> workers = new HashSet<>();
    private final SortedSet<Long> times = new TreeSet<>();

    public void add(long time, long worker, WorkerStatusBL status) {
        ensureWorker(worker);
        times.add(time);

        WorkerStatusHolder globalStatus = currentStatus.get(worker);
        globalStatus = globalStatus.merge(status);
        currentStatus.put(worker, globalStatus);
        timeline.get(worker).add(time, globalStatus);
    }

    public void add(long time, long worker, WorkerStatusPK status) {
        ensureWorker(worker);
        times.add(time);

        WorkerStatusHolder globalStatus = currentStatus.get(worker);
        globalStatus = globalStatus.merge(status);
        currentStatus.put(worker, globalStatus);
        timeline.get(worker).add(time, globalStatus);
    }

    public void add(long time, long worker, WorkerStatusJN status) {
        ensureWorker(worker);
        times.add(time);

        WorkerStatusHolder globalStatus = currentStatus.get(worker);
        globalStatus = globalStatus.merge(status);
        currentStatus.put(worker, globalStatus);
        timeline.get(worker).add(time, globalStatus);
    }

    public void markInvalid(long time, long worker) {
        timeline.get(worker).removeBefore(time);
    }

    private void ensureWorker(long worker) {
        if (workers.add(worker)) {
            Timeline<WorkerStatusHolder> tl = new Timeline<>();
            tl.add(-1, WorkerStatusHolder.DEFAULT);
            timeline.put(worker, tl);
            currentStatus.put(worker, WorkerStatusHolder.DEFAULT);
        }
    }

    public SortedSet<Long> getTimes() {
        return times;
    }

    public WorkerStatusHolder getStatus(long worker, long time) {
        Timeline<WorkerStatusHolder> tl = timeline.get(worker);
        if (tl == null) {
            return WorkerStatusHolder.UNKNOWN;
        } else {
            WorkerStatusHolder status = tl.getStatus(time);
            return (status == null) ? WorkerStatusHolder.UNKNOWN : status;
        }
    }


}
