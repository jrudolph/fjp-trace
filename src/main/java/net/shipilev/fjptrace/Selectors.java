package net.shipilev.fjptrace;

import java.awt.*;

public class Selectors {

    private static final Color COLOR_GRAY = Color.getHSBColor(0f, 0f, 0.9f);

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
                        return COLOR_GRAY;
                }
            case RUNNING:
                switch (jnStatus) {
                    case FREE:
                        switch (pkStatus) {
                            case ACTIVE:
                                return Color.GREEN;
                            case PARKED:
                                return Color.MAGENTA;
                        }
                    case JOINING:
                        switch (pkStatus) {
                            case ACTIVE:
                                return Color.BLUE;
                            case PARKED:
                                return Color.RED;
                        }
                }
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
        }
        throw new IllegalStateException();
    }
}
