package net.shipilev.fjptrace;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

public class TraceTextTask extends RecursiveAction {

    private static final String TRACE_TEXT = System.getProperty("trace.text", "trace.log.gz");
    private static final Integer TRACE_TEXT_LIMIT = Integer.getInteger("trace.text.limit", 100_000);

    private final Events events;
    private final WorkerStatus workerStatus;

    public TraceTextTask(Events events, WorkerStatus workerStatus) {
        this.events = events;
        this.workerStatus = workerStatus;
    }

    @Override
    public void compute() {
        try {
            call();
        } catch (Exception e) {
            // do nothing
        }
    }

    public void call() throws Exception {
        int count = 0;
        List<Event> list = events.getList();

        int linesToProcess;
        if (list.size() > TRACE_TEXT_LIMIT) {
            System.out.println("Impractical to dump text trace larger than for " + TRACE_TEXT_LIMIT + ", limiting output");
            linesToProcess = TRACE_TEXT_LIMIT;
        } else {
            linesToProcess = list.size();
        }

        System.out.println("Rendering text to " + TRACE_TEXT);

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
