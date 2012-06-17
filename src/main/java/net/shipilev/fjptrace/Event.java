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

public class Event implements Comparable<Event> {
    public final long time;
    public final EventType eventType;
    public final Worker worker;
    public final int taskHC;

    public Event(Long time, EventType eventType, Worker worker, int taskHC) {
        this.time = time;
        this.eventType = eventType;
        this.worker = worker;
        this.taskHC = taskHC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Event event = (Event) o;

        if (time != event.time) {
            return false;
        }
        if (eventType != event.eventType) {
            return false;
        }
        if (taskHC != event.taskHC) {
            return false;
        }
        if (worker != null ? !worker.equals(event.worker) : event.worker != null) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(Event o2) {
        Event o1 = this;
        int r = Long.compare(o1.time, o2.time);
        if (r != 0)
            return r;

        int r1 = Long.compare(o1.worker.id, o2.worker.id);
        if (r1 != 0)
            return r1;

        int r2 = Long.compare(o1.taskHC, o2.taskHC);
        if (r2 != 0)
            return r2;

        int r3 = Integer.compare(o1.eventType.ordinal(), o2.eventType.ordinal());
        if (r3 != 0)
            return r3;

        return 0;
    }

    @Override
    public int hashCode() {
        int result = (int) (time ^ (time >>> 32));
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (worker != null ? worker.hashCode() : 0);
        result = 31 * result + taskHC;
        return result;
    }
}