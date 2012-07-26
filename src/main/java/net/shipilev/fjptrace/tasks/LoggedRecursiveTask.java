package net.shipilev.fjptrace.tasks;

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
            reportProgress(0);
            result = doWork();
            reportProgress(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    protected PrintWriter getPw() {
        return pw;
    }

    protected void reportProgress(double fraction) {
        pw.printf("%9.2f%%: %s\n", fraction*100, description);
    }

    abstract V doWork() throws Exception;

}
