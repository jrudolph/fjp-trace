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

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class PrintEventsTask extends LoggedRecursiveAction {

    private final Events events;
    private final String filename;

    public PrintEventsTask(Options opts, Events events) {
        super("Dump events");
        this.filename = opts.getTargetPrefix() + "-events.txt.gz";
        this.events = events;
    }

    @Override
    public void doWork() throws Exception {
        int count = 0;
        List<Event> list = events.getList();

        PrintWriter pw = new PrintWriter(new GZIPOutputStream(new FileOutputStream(filename)));
        for (Event e : list) {
            if ((count++ & 0xFFFF) == 0) {
                reportProgress(count*1.0 / list.size());
            }
            pw.println(e);
        }
        pw.close();
    }
}
