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
    FORK,

    /**
     * Task is being joined.
     */
    JOIN,

    /**
     * Task has joined.
     */
    JOINED,

    /**
     * Task is about to be executed.
     */
    EXEC,

    /**
     * Task has executed.
     */
    EXECUTED,

    /**
     * Task is about to be invoked.
     */
    INVOKE,

    /**
     * Task has been invoked.
     */
    INVOKED,

    /**
     * Worker is about to park.
     */
    PARK,

    /**
     * Worker to be unparked.
     */
    UNPARK,

    /**
     * Worker had just unparked.
     */
    UNPARKED,

    /**
     * Worker to start waiting
     */
    WAIT,

    /**
     * Worker had completed waiting
     */
    WAITED,

    /**
     * Task was submitted.
     */
    SUBMIT,

    /**
     * Tracer is blocked (e.g. waiting for event dump)
     */
    TRACE_BLOCK,

    /**
     * Tracer is unblocked (e.g. waiting for event dump)
     */
    TRACE_UNBLOCK,
}
