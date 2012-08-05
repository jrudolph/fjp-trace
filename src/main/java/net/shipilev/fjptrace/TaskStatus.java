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

package net.shipilev.fjptrace;

import net.shipilev.fjptrace.util.PairedList;

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
