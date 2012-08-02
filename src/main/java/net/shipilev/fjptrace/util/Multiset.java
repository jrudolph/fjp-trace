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

import java.util.Collection;
import java.util.HashMap;

/**
 * Dirty and naive multiset implementation.
 * (Should really used Guava, but pulling 1 Mb just for single class is dumb).
 *
 * @param <T>
 */
public class Multiset<T> {

    private final HashMap<T, Long> counts = new HashMap<>();

    public long add(T t) {
        return add(t, 1);
    }

    public long add(T t, long inc) {
        Long count = counts.get(t);
        if (count == null) {
            count = 0L;
        }
        count += inc;
        counts.put(t, count);
        return count;
    }

    public void remove(T t, long i) {
        Long count = counts.get(t);
        if (count == null) {
            return;
        }

        count -= i;

        if (count < 0) {
            throw new IllegalStateException("Negative size");
        }

        if (count == 0) {
            counts.remove(t);
        } else {
            counts.put(t, count);
        }
    }

    public boolean contains(T t) {
        return counts.containsKey(t);
    }

    public long count(T t) {
        Long v = counts.get(t);
        return v == null ? 0 : v;
    }

    public void removeKey(T t) {
        counts.remove(t);
    }

    public void addAll(Collection<T> ts) {
        for (T t : ts) {
            add(t);
        }
    }

    public T getMostFrequent() {
        long maxCount = Long.MIN_VALUE;
        T mostFrequent = null;
        for (T t : counts.keySet()) {
            Long count = counts.get(t);
            if (count > maxCount) {
                maxCount = count;
                mostFrequent = t;
            }
        }
        return mostFrequent;
    }

    public Collection<T> keys() {
        return counts.keySet();
    }
}
