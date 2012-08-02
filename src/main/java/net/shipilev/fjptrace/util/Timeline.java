/*
 * Copyright 2012 Aleksey Shipilev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shipilev.fjptrace.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Timeline<T> {

    private final List<Tick> ticks = new ArrayList<>();

    private volatile boolean isSorted;

    public void add(long time, T status) {
        ticks.add(new Tick(time, status));
        isSorted = false;
    }

    public T getStatus(long time) {
        if (!isSorted) {
            Collections.sort(ticks);
            isSorted = true;
        }

        int i = Collections.binarySearch(ticks, new Tick(time, null));
        if (i > 0) {
            return ticks.get(i).status;
        }

        int insertionPoint = -i - 1;

        // don't speculate about the past
        if (insertionPoint <= 0) {
            return null;
        }

        // don't speculate about the future
        if (insertionPoint == ticks.size()) {
            return null;
        }

        return ticks.get(insertionPoint - 1).status;
    }

    public void removeBefore(long time) {
        // FIXME: Crude and inefficient

        List<Tick> newTicks = new ArrayList<>();
        for (Tick t : ticks) {
            if (t.time > time) {
                newTicks.add(t);
            }
        }

        ticks.clear();
        ticks.addAll(newTicks);
    }

    private class Tick implements Comparable<Tick> {
        private final long time;
        private final T status;

        public Tick(long time, T status) {
            this.time = time;
            this.status = status;
        }

        @Override
        public int compareTo(Tick o) {
            return Long.compare(time, o.time);
        }
    }
}
