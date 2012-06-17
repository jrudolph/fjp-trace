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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Worker implements Comparable<Worker> {
    public final long id;

    public Worker(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Worker worker = (Worker) o;

        if (id != worker.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public int compareTo(Worker o) {
        return Long.compare(id, o.id);
    }

    public static Map<Long, Worker> cache = new HashMap<>();

    public static Worker newInstance(long threadID) {
        Worker w = cache.get(threadID);
        if (w == null) {
            w = new Worker(threadID);
            cache.put(threadID, w);
        }
        return w;
    }
}
