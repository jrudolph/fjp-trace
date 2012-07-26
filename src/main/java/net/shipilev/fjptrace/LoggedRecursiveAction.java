package net.shipilev.fjptrace;

import java.io.PrintWriter;
import java.util.concurrent.RecursiveAction;

public abstract class LoggedRecursiveAction extends RecursiveAction {

    private final String description;
    private final PrintWriter pw;

    public LoggedRecursiveAction(String description) {
        super();
        this.description = description;
        this.pw = new PrintWriter(System.out, true);
    }

    @Override
    protected void compute() {
        try {
            reportProgress(0);
            doWork();
            reportProgress(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected PrintWriter getPw() {
        return pw;
    }

    protected void reportProgress(double fraction) {
        pw.printf("%9.2f%%: %s\n", fraction*100, description);
    }

    abstract void doWork() throws Exception;
}
