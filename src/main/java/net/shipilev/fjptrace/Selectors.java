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

import java.awt.*;

public class Selectors {

    public static final Color COLOR_LIGHT_GRAY = new Color(240, 240, 240);
    public static final Color COLOR_DARK_GRAY = new Color(200, 200, 200);
    public static final Color COLOR_DARK_GREEN = new Color(0, 128, 0);
    public static final Color COLOR_LIGHT_RED = new Color(255, 128, 128);

    private Selectors() {
        // prevent instantiation
    }

    public static Color selectColor(WorkerStatusBL blStatus, WorkerStatusPK pkStatus, WorkerStatusJN jnStatus) {
        switch (blStatus) {
            case IDLE:
                switch (pkStatus) {
                    case ACTIVE:
                        return Color.YELLOW;
                    case PARKED:
                        return COLOR_DARK_GRAY;
                }
            case RUNNING:
                switch (jnStatus) {
                    case FREE:
                        switch (pkStatus) {
                            case ACTIVE:
                                return Color.GREEN;
                            case PARKED:
                                return COLOR_LIGHT_RED;
                        }
                    case JOINING:
                        switch (pkStatus) {
                            case ACTIVE:
                                return COLOR_DARK_GREEN;
                            case PARKED:
                                return Color.RED;
                        }
                }
            case UNKNOWN:
                return Color.BLACK;

        }
        throw new IllegalStateException();
    }

    // should be exactly 20 chars
    public static String selectText(WorkerStatusBL blStatus, WorkerStatusPK pkStatus, WorkerStatusJN jnStatus) {
        switch (blStatus) {
            case IDLE:
                switch (pkStatus) {
                    case ACTIVE:
                        return "    --- infr ---    ";
                    case PARKED:
                        return "                    ";
                }
            case RUNNING:
                switch (jnStatus) {
                    case FREE:
                        switch (pkStatus) {
                            case ACTIVE:
                                return "    *** exec ***    ";
                            case PARKED:
                                return "    --- wait ---    ";
                        }
                    case JOINING:
                        switch (pkStatus) {
                            case ACTIVE:
                                return "    *** join ***    ";
                            case PARKED:
                                return "    --- wait ---    ";
                        }
                }
            case UNKNOWN:
                return "       ??????       ";
        }
        throw new IllegalStateException();
    }

    public static String selectTextLong(WorkerStatusBL blStatus, WorkerStatusPK pkStatus, WorkerStatusJN jnStatus) {
        switch (blStatus) {
            case IDLE:
                switch (pkStatus) {
                    case ACTIVE:
                        return "Infrastructure (everything beyond tasks themselves)";
                    case PARKED:
                        return "Parked, and no work";
                }
            case RUNNING:
                switch (jnStatus) {
                    case FREE:
                        switch (pkStatus) {
                            case ACTIVE:
                                return "Executing local task, running";
                            case PARKED:
                                return "Executing local task, parked on waiting";
                        }
                    case JOINING:
                        switch (pkStatus) {
                            case ACTIVE:
                                return "Joining task, executing another task";
                            case PARKED:
                                return "Joining task, executing another task, parked on waiting";
                        }
                }
            case UNKNOWN:
                return "No data (buffers had not yet flushed?)";
        }
        throw new IllegalStateException();
    }
}
