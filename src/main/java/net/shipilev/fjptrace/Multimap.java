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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Dirty and naive multimap implementation.
 * (Should really used Guava, but pulling 1 Mb just for single class is dumb).
 *
 * @param <K>
 * @param <V>
 */
public class Multimap<K, V> {

    private final HashMap<K, List<V>> map = new HashMap<>();

    public void put(K k, V v) {
        List<V> list = map.get(k);
        if (list == null) {
            list = new ArrayList<>();
            map.put(k, list);
        }
        list.add(v);
    }

    public Collection<K> keySet() {
        return map.keySet();
    }

    public Collection<V> get(K k) {
        List<V> vs = map.get(k);
        return vs == null ? Collections.<V>emptyList() : Collections.unmodifiableCollection(vs);
    }

}
