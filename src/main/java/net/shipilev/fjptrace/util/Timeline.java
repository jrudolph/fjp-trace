/*
 * Copyright (c) 2012 Aleksey Shipilev
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
import java.util.Collections;
import java.util.List;

public class Timeline<T> {

    private List<Tick<T>> ticks = new ArrayList<>();

    private volatile boolean isSorted;

    private int lastFound;

    public void add(long time, T status) {
        ticks.add(new Tick<>(time, status));
        isSorted = false;
    }

    public T getStatus(long time, boolean makePrediction) {
        if (!isSorted) {
            List<Tick<T>> newTicks = new ArrayList<>(ticks);
            Collections.sort(newTicks);
            ticks = newTicks;
            isSorted = true;
        }

        int i = binarySearch(ticks, new Tick<T>(time, null), lastFound);
        if (i >= 0) {
            lastFound = i;
            return ticks.get(lastFound).status;
        }

        int insertionPoint = -i - 1;

        // don't speculate about the past
        if (insertionPoint <= 0) {
            return null;
        }

        // don't speculate about the future
        if (!makePrediction && insertionPoint == ticks.size()) {
            return null;
        }

        lastFound = insertionPoint - 1;
        return ticks.get(lastFound).status;
    }

    public <T extends Comparable<T>> int binarySearch(List<T> list, T key, int guess) {
        return binarySearch(list, 0, list.size(), key, guess);
    }

    public <T extends Comparable<T>> int binarySearch(List<T> list, int from, int to, T key, int guess) {
        if ((guess < to) && key.compareTo(list.get(guess)) == 0) {
            return guess;
        }

        if ((guess + 1 < to) && key.compareTo(list.get(guess + 1)) < 0 && key.compareTo(list.get(guess)) > 0 ) {
            return guess;
        }

        int low = from;
        int high = to - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = list.get(mid);

            int c = midVal.compareTo(key);
            if (c < 0)
                low = mid + 1;
            else if (c > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }


    public T getStatus(long time) {
        return getStatus(time, false);
    }

    public void removeBefore(long time) {
        // FIXME: Crude and inefficient

        List<Tick<T>> newTicks = new ArrayList<>();
        for (Tick<T> t : ticks) {
            if (t.time > time) {
                newTicks.add(t);
            }
        }

        ticks.clear();
        ticks.addAll(newTicks);
    }


    private static class Tick<T> implements Comparable<Tick<T>> {
        private final long time;
        private final T status;

        public Tick(long time, T status) {
            this.time = time;
            this.status = status;
        }

        @Override
        public int compareTo(Tick<T> o) {
            return Long.compare(time, o.time);
        }
    }
}
