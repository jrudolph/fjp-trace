package net.shipilev.fjptrace;

import java.awt.*;

public class Selectors {

    private static final Color COLOR_LIGHT_GRAY = new Color(240, 240, 240);
    private static final Color COLOR_DARK_GRAY = new Color(200, 200, 200);
    private static final Color COLOR_DARK_GREEN = new Color(0, 128, 0);
    private static final Color COLOR_LIGHT_RED = new Color(255, 128, 128);

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
