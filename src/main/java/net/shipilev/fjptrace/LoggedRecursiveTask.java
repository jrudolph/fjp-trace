package net.shipilev.fjptrace;

import java.io.PrintWriter;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public abstract class LoggedRecursiveTask<V> extends RecursiveTask<V> {

    private final String description;
    private final PrintWriter pw;

    public LoggedRecursiveTask(String description) {
        super();
        this.description = description;
        this.pw = new PrintWriter(System.out, true);
    }

    @Override
    protected V compute() {
        V result = null;
        try {
            pw.printf("%10s: %s\n", "start", description);
            result = doWork();
            pw.printf("%10s: %s\n", "complete", description);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    protected PrintWriter getPw() {
        return pw;
    }

    abstract V doWork() throws Exception;
}
