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

package net.shipilev.fjptrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Timeline<T extends Enum<?>> {

    private final SortedSet<Tick> ticks = new TreeSet<>();

    public void add(long time, T status) {
        ticks.add(new Tick(time, status));
    }

    public T getStatus(long time) {
        SortedSet<Tick> set = ticks.headSet(new Tick(time, null));
        if (!set.isEmpty()) {
            return set.last().status;
        } else {
            if (!ticks.isEmpty()) {
                return ticks.first().status;
            } else {
                return null;
            }
        }
    }

    public Collection<Long> getTimes() {
        List<Long> result = new ArrayList<>();
        for (Tick tick : ticks) {
            result.add(tick.time);
        }
        return result;
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
