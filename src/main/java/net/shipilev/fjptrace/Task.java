package net.shipilev.fjptrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Task {
    private final List<Task> children;
    private final List<Event> events;
    private final int taskTag;

    private volatile int depth;
    private long time;
    private long selfTime;
    private long totalTime;
    private long worker;

    public Task(int taskTag) {
        this.events = new ArrayList<>();
        this.taskTag = taskTag;
        this.depth = -1;
        children = new ArrayList<>();
    }

    public void addChild(Task child) {
        children.add(child);
    }

    public int getDepth() {
        return depth;
    }

    public Collection<Task> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void addSelf(long time, long duration) {
        this.time = time;
        this.selfTime = duration;
    }

    public void addTotal(long time, long duration) {
        this.time = time;
        this.totalTime = duration;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getTime() {
        return time;
    }

    public long getSelfTime() {
        return selfTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Task task = (Task) o;

        if (taskTag != task.taskTag) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return taskTag;
    }

    public void setWorker(long worker) {
        this.worker = worker;
    }

    public long getWorker() {
        return worker;
    }

    public void recordEvent(Event e) {
        events.add(e);
    }

    public List<Event> getEvents() {
        return events;
    }
}
