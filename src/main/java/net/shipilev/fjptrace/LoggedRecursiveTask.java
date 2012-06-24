package net.shipilev.fjptrace;

import java.io.PrintWriter;
import java.util.concurrent.RecursiveAction;

public abstract class LoggedRecursiveTask extends RecursiveAction {

    private final String description;
    private final PrintWriter pw;

    public LoggedRecursiveTask(String description) {
        super();
        this.description = description;
        this.pw = new PrintWriter(System.out);
    }

    @Override
    protected void compute() {
        try {
            pw.printf("%10s: %s\n", "start", description);
            doWork();
            pw.printf("%10s: %s\n", "complete", description);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected PrintWriter getPw() {
        return pw;
    }

    abstract void doWork() throws Exception;
}
