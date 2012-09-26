package net.shipilev.fjptrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Task {
    private final List<Task> children = new ArrayList<>();
    private int depth;

    public Task(int depth) {
        this.depth = depth;
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
}
