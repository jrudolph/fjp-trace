package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Selectors;
import net.shipilev.fjptrace.WorkerStatus;
import net.shipilev.fjptrace.WorkerStatusHolder;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

public class DumpEventStreamTask extends LoggedRecursiveAction {

    private static final String TRACE_DUMP = System.getProperty("trace.dump", "events.data.gz");

    private final Events events;

    public DumpEventStreamTask(Events events) {
        super("Printing task trace to " + TRACE_DUMP);
        this.events = events;
    }

    @Override
    public void doWork() throws Exception {
        int count = 0;
        List<Event> list = events.getList();

        PrintWriter pw = new PrintWriter(new GZIPOutputStream(new FileOutputStream(TRACE_DUMP)));
        for (Event e : list) {
            if ((count++ & 0xFFFF) == 0) {
                reportProgress(count*1.0 / list.size());
            }
            pw.println(e);
        }
        pw.close();
    }
}
