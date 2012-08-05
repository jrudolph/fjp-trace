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
import net.shipilev.fjptrace.tasks.PrintWorkerStateTask;
import net.shipilev.fjptrace.tasks.RenderExternalTaskColoringTask;
import net.shipilev.fjptrace.tasks.RenderTaskExecTimeTask;
import net.shipilev.fjptrace.tasks.RenderWorkerQueueTask;
import net.shipilev.fjptrace.tasks.ReadTask;
import net.shipilev.fjptrace.tasks.RenderWorkerStateTask;
import net.shipilev.fjptrace.tasks.TaskStatusTask;
import net.shipilev.fjptrace.tasks.TaskSubgraphTask;
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

            WorkerStatusTask wStatus = new WorkerStatusTask(events);
            wStatus.fork();

            TaskStatusTask tStatus = new TaskStatusTask(events);
            tStatus.fork();

            WorkerQueueStatusTask wqStatus = new WorkerQueueStatusTask(events);
            wqStatus.fork();

            TaskSubgraphTask tsStatus = new TaskSubgraphTask(events);
            tsStatus.fork();

            new CheckEventsTask(events).invoke();

            ForkJoinTask.invokeAll(
                    new RenderExternalTaskColoringTask(opts, events, tsStatus.join()),
                    new RenderWorkerQueueTask(opts, events, wqStatus.join()),
                    new RenderWorkerStateTask(opts, events, wStatus.join()),
                    new PrintEventsTask(opts, events),
                    new PrintWorkerStateTask(opts, events, wStatus.join()),
                    new RenderTaskExecTimeTask(opts, events, tStatus.join())
            );
        }
    }

}



