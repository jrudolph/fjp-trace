package net.shipilev.fjptrace;

public class TaskStatus {

    private final PairedList selfDurations = new PairedList();
    private final PairedList totalDurations = new PairedList();


    public void addSelf(long timestamp, long duration) {
        selfDurations.add(timestamp, duration);
    }

    public void addTotal(long timestamp, long duration) {
        totalDurations.add(timestamp, duration);
    }

    public PairedList getSelf() {
        return selfDurations;
    }

    public PairedList getTotal() {
        return totalDurations;
    }
}
