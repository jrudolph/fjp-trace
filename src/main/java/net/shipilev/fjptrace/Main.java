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

import net.shipilev.fjptrace.tasks.CheckEventsTask;
import net.shipilev.fjptrace.tasks.PrintEventsTask;
import net.shipilev.fjptrace.tasks.PrintSummaryTask;
import net.shipilev.fjptrace.tasks.PrintWorkerStateTask;
import net.shipilev.fjptrace.tasks.RenderExternalTaskColoringTask;
import net.shipilev.fjptrace.tasks.RenderTaskExecTimeTask;
import net.shipilev.fjptrace.tasks.RenderWorkerQueueTask;
import net.shipilev.fjptrace.tasks.ReadTask;
import net.shipilev.fjptrace.tasks.RenderWorkerStateTask;
import net.shipilev.fjptrace.tasks.TaskStatusTask;
import net.shipilev.fjptrace.tasks.TraceBlockEstimatesTask;
import net.shipilev.fjptrace.tasks.WorkerQueueStatusTask;
import net.shipilev.fjptrace.tasks.WorkerStatusTask;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class Main {

    public static void main(String[] args) throws IOException {
        PrintStream out = System.out;

        out.println("Instrumented ForkJoinPool: trace renderer");
        out.println("  This is a free software, and it comes with ABSOLUTELY NO WARRANTY.");
        out.println("  This software includes the complete implementation of ForkJoinPool by Doug Lea and other JSR166 EG members.");
        out.println("  Bug reports, RFEs, suggestions, and success stories are welcome at https://github.com/shipilev/fjp-trace/");
        out.println();

        Options opts = new Options(args);

        if (!opts.parse()) {
            System.exit(1);
        }

        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new MainTask(opts));
    }

    private static class MainTask extends RecursiveAction {

        private final Options opts;

        public MainTask(Options opts) {
            this.opts = opts;
        }

        @Override
        protected void compute() {
            Events events = new ReadTask(opts).invoke();

            /*
               We are doing the tasks per stride, because we try to minimize heap impact.
               This also explains silly nulls assigned for the tasks afterwards.
             */

            CheckEventsTask checkEvents = new CheckEventsTask(events);
            PrintEventsTask printEventsTask = new PrintEventsTask(opts, events);
            TraceBlockEstimatesTask traceEstimateTask = new TraceBlockEstimatesTask(events);
            traceEstimateTask.fork();
            checkEvents.fork();
            printEventsTask.fork();

            {
                TaskStatusTask tStatus = new TaskStatusTask(events);
                tStatus.fork();
                ForkJoinTask.invokeAll(
                        new RenderExternalTaskColoringTask(opts, events, tStatus.join()),
                        new RenderTaskExecTimeTask(opts, events, tStatus.join()),
                        new PrintSummaryTask(opts, tStatus.join())
                        );
                tStatus = null;
            }

            {
                WorkerStatusTask wStatus = new WorkerStatusTask(events);
                wStatus.fork();
                ForkJoinTask.invokeAll(
                        new RenderWorkerStateTask(opts, events, wStatus.join()),
                        new PrintWorkerStateTask(opts, events, wStatus.join())
                        );
                wStatus = null;
            }

            {
                WorkerQueueStatusTask wqStatus = new WorkerQueueStatusTask(events);
                wqStatus.fork();
                ForkJoinTask.invokeAll(
                        new RenderWorkerQueueTask(opts, events, wqStatus.join())
                        );
                wqStatus = null;
            }

            traceEstimateTask.join();
            checkEvents.join();
            printEventsTask.join();

        }
    }

}



