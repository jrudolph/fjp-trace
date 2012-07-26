/*
 * Copyright 2012 Aleksey Shipilev
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

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        String filename = "forkjoin.trace";
        if (args.length >= 1) {
            filename = args[0];
        }

        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new MainTask(filename));
    }

    private static class MainTask extends RecursiveAction {

        private final String filename;

        public MainTask(String filename) {
            this.filename = filename;
        }

        @Override
        protected void compute() {
            Events events = new ReadTask(filename).invoke();

            WorkerStatusTask wStatus = new WorkerStatusTask(events);
            wStatus.fork();

            TaskStatusTask tStatus = new TaskStatusTask(events);
            tStatus.fork();

            ForkJoinTask.invokeAll(
                    new TraceGraphTask(events, wStatus.join()),
                    new TraceTextTask(events, wStatus.join()),
                    new TaskStatsRenderTask(events, tStatus.join()),
                    new WorkerQueueStatusTask(events)
            );
        }
    }

}



