package net.shipilev.fjptrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Task {
    private final List<Task> children = new ArrayList<>();

    public void addChild(Task child) {
        children.add(child);
    }

    public Collection<Task> getChildren() {
        return Collections.unmodifiableCollection(children);
    }
}
