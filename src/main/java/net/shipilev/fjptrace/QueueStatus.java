package net.shipilev.fjptrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QueueStatus {

    private final List<Long> externalSubmissions = new ArrayList<>();

    public void registerAdd(long time, long worker) {
        // TODO: stub
    }

    public void registerSteal(long time, long worker) {
        // TODO: stub
    }

    public void registerExec(long time, long worker) {
        // TODO: stub
    }

    public void registerExternal(long time, long worker) {
        externalSubmissions.add(time);
    }

    public Collection<Long> getTimes() {
        return externalSubmissions;
    }
}
