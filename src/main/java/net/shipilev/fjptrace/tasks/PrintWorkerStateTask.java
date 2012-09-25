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
import net.shipilev.fjptrace.Options;
import net.shipilev.fjptrace.Selectors;
import net.shipilev.fjptrace.WorkerStatus;
import net.shipilev.fjptrace.WorkerStatusHolder;
import net.shipilev.fjptrace.util.GZIPOutputStreamEx;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

public class PrintWorkerStateTask extends LoggedRecursiveAction {

    private static final Integer TRACE_TEXT_LIMIT = Integer.getInteger("trace.text.limit", 100_000);

    private final Events events;
    private final WorkerStatus workerStatus;
    private final String filename;

    public PrintWorkerStateTask(Options opts, Events events, WorkerStatus workerStatus) {
        super("Worker state trace");
        this.filename = opts.getTargetPrefix() + "-workerState.txt.gz";
        this.events = events;
        this.workerStatus = workerStatus;
    }

    @Override
    public void doWork() throws Exception {
        int count = 0;
        List<Event> list = events.getList();

        int linesToProcess;
        if (list.size() > TRACE_TEXT_LIMIT) {
            getPw().println("Impractical to dump text trace larger than for " + TRACE_TEXT_LIMIT + ", limiting output");
            linesToProcess = TRACE_TEXT_LIMIT;
        } else {
            linesToProcess = list.size();
        }

        PrintWriter pw = new PrintWriter(new GZIPOutputStreamEx(new FileOutputStream(filename)));

        pw.format("%10s", "Time, ms");
        for (long w : events.getWorkers()) {
            pw.format("%20s", w);
        }
        pw.println();

        for (Event e : list) {
            if (count++ > linesToProcess) {
                break;
            }

            if ((count & 0xFFFF) == 0) {
                reportProgress(count*1.0 / list.size());
            }

            pw.format("%10d", TimeUnit.NANOSECONDS.toMillis(e.time));

            for (long w : events.getWorkers()) {
                if (w == e.workerId) {
                    pw.format("%20s", e.eventType + "(" + e.taskTag + ")");
                } else {
                    WorkerStatusHolder status = workerStatus.getStatus(w, e.time);
                    pw.print(Selectors.selectText(status.blStatus, status.pkStatus, status.jnStatus));
                }
            }
            pw.println();
        }
        pw.close();
    }
}
