package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Selectors;
import net.shipilev.fjptrace.WorkerStatus;
import net.shipilev.fjptrace.WorkerStatusBL;
import net.shipilev.fjptrace.WorkerStatusJN;
import net.shipilev.fjptrace.WorkerStatusPK;
import net.shipilev.fjptrace.tasks.LoggedRecursiveAction;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

public class TraceTextTask extends LoggedRecursiveAction {

    private static final String TRACE_TEXT = System.getProperty("trace.text", "trace.log.gz");
    private static final Integer TRACE_TEXT_LIMIT = Integer.getInteger("trace.text.limit", 100_000);

    private final Events events;
    private final WorkerStatus workerStatus;

    public TraceTextTask(Events events, WorkerStatus workerStatus) {
        super("Printing task trace to " + TRACE_TEXT);
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

        PrintWriter pw = new PrintWriter(new GZIPOutputStream(new FileOutputStream(TRACE_TEXT)));

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
                    pw.format("%20s", e.eventType + "(" + e.taskHC + ")");
                } else {
                    WorkerStatusBL statusBL = workerStatus.getBLStatus(w, e.time);
                    WorkerStatusPK statusPK = workerStatus.getPKStatus(w, e.time);
                    WorkerStatusJN statusJN = workerStatus.getJNStatus(w, e.time);

                    pw.print(Selectors.selectText(statusBL, statusPK, statusJN));
                }
            }
            pw.println();
        }
        pw.close();
    }
}
