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

public enum EventType {

    /**
     * Task is being forked.
     */
    FORK(Target.TASK),

    /**
     * Task is being joined.
     */
    JOIN(Target.TASK),

    /**
     * Task has joined.
     */
    JOINED(Target.TASK),

    /**
     * Task is about to be executed.
     */
    EXEC(Target.TASK),

    /**
     * Task has executed.
     */
    EXECUTED(Target.TASK),

    /**
     * Task is about to be invoked.
     */
    INVOKE(Target.TASK),

    /**
     * Task has been invoked.
     */
    INVOKED(Target.TASK),

    /**
     * Worker is about to park.
     */
    PARK(Target.THREAD),

    /**
     * Worker to be unparked.
     */
    UNPARK(Target.THREAD),

    /**
     * Worker had just unparked.
     */
    UNPARKED(Target.THREAD),

    /**
     * Worker to start waiting
     */
    WAIT(Target.THREAD),

    /**
     * Worker had completed waiting
     */
    WAITED(Target.THREAD),

    /**
     * Task was submitted.
     */
    SUBMIT(Target.TASK),

    /**
     * Tracer is blocked (e.g. waiting for event dump)
     */
    TRACE_BLOCK(Target.THREAD),

    /**
     * Tracer is unblocked (e.g. waiting for event dump)
     */
    TRACE_UNBLOCK(Target.THREAD),

    /**
     * Completer had started
     */
    COMPLETING(Target.TASK),

    /**
     * Completer finished
     */
    COMPLETED(Target.TASK);

    private final Target target;

    EventType(Target target) {
        this.target = target;
    }

    public Target target() {
        return target;
    }

    public enum Target {
        TASK,
        THREAD
    }

}
