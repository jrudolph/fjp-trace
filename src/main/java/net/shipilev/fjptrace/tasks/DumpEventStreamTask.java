package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Options;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class DumpEventStreamTask extends LoggedRecursiveAction {

    private final Events events;
    private final String filename;

    public DumpEventStreamTask(Options opts, Events events) {
        super("Task trace");
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
