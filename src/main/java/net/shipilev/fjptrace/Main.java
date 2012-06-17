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

import sun.misc.Unsafe;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final int HEIGHT = Integer.getInteger("height", 2000);
    private static final int WIDTH = Integer.getInteger("width", 1000);
    private static final int OFFSET = Integer.getInteger("offset", 0);
    private static final int LIMIT = Integer.getInteger("limit", Integer.MAX_VALUE);
    private static final String TRACE_TEXT = System.getProperty("trace.text", "trace.log");
    private static final String TRACE_GRAPH = System.getProperty("trace.graph", "trace.png");

    private List<Event> events = new ArrayList<>();
    private SortedSet<Worker> workers = new TreeSet<>();

    private Map<Worker,Timeline<WorkerStatusBL>> blTimelines = new HashMap<>();
    private Map<Worker,Timeline<WorkerStatusPK>> pkTimelines = new HashMap<>();
    private Map<Worker,Timeline<WorkerStatusJN>> jnTimelines = new HashMap<>();
    private final static Unsafe U;
    private final static long BBASE;
    private long start;
    private long end;

    public static void main(String[] args) throws IOException {
        String filename = "forkjoin.trace";
        if (args.length >= 1) {
            filename = args[0];
        }
        new Main().run(filename);
    }

    static {
        // steal Unsafe
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
            BBASE = U.arrayBaseOffset(byte[].class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

    }

    private void run(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));

        byte[] buffer = new byte[22];

        int count = 0;
        while (is.read(buffer) == 22) {
            count++;
            if (count < OFFSET) {
                continue;
            }
            if (count > OFFSET + LIMIT) {
                break;
            }

            long time = U.getLong(buffer, BBASE + 0);
            int eventOrd = U.getShort(buffer, BBASE + 8);
            int taskHC = U.getInt(buffer, BBASE + 10);
            long threadID = U.getLong(buffer, BBASE + 14);

            Worker worker = Worker.newInstance(threadID);

            Event event = new Event(time, EventType.values()[eventOrd], worker, taskHC);
            if (!events.add(event)) {
                throw new IllegalStateException("Duplicate event: " + event);
            }

            workers.add(worker);
        }

        Collections.sort(events);

        start = events.get(0).time;
        end = events.get(events.size() - 1).time;

        computeWorkerStatus();

        renderGraph();
        renderText();
        computeStats();
    }

    private void renderGraph() throws IOException {
        System.out.println("Rendering graph to " + TRACE_GRAPH);


        final int H_HEIGHT = (int) (200);
        final int D_HEIGHT = (int) (HEIGHT - H_HEIGHT);

        final int T_WIDTH = (int) (WIDTH * 0.1);
        final int W_WIDTH = (int) (WIDTH * 0.65);
        final int P_WIDTH = (int) (WIDTH * 0.05);
        final int D_WIDTH = (int) (WIDTH * 0.2);

        final int W_STEP = W_WIDTH / workers.size();
        final int D_STEP = D_WIDTH / workers.size();

        /*
          Compute pivot points
        */
        SortedSet<Long> times = new TreeSet<>();

        for (Timeline t : blTimelines.values()) {
            times.addAll(t.getTimes());
        }
        for (Timeline t : pkTimelines.values()) {
            times.addAll(t.getTimes());
        }

        /*
           Render it!
         */

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setBackground(Color.WHITE);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        long lastTick = start;
        for (long tick : times) {

            int cY = H_HEIGHT + (int) (D_HEIGHT * (tick - start) / (end - start));
            int lY = H_HEIGHT + (int) (D_HEIGHT * (lastTick - start) / (end - start));

            List<Color> colors = new ArrayList<>();

            int wIndex = 0;
            for (Worker w : workers) {
                WorkerStatusBL blStatus = blTimelines.get(w).getStatus(tick);
                WorkerStatusPK pkStatus = pkTimelines.get(w).getStatus(tick);
                WorkerStatusJN jnStatus = jnTimelines.get(w).getStatus(tick);

                Color color = selectColor(blStatus, pkStatus, jnStatus);
                colors.add(color);

                g.setColor(color);

                int cX = T_WIDTH + wIndex * W_STEP;
                g.fillRect(cX, lY, W_STEP, cY - lY);

                wIndex++;
            }

            Collections.sort(colors, new Comparator<Color>() {
                @Override
                public int compare(Color o1, Color o2) {
                    return Integer.compare(o1.getRGB(), o2.getRGB());
                }
            });

            int cIndex = 0;
            for (Color c : colors) {
                g.setColor(c);
                g.fillRect(T_WIDTH + W_WIDTH + P_WIDTH + cIndex*D_STEP, lY, D_STEP, cY - lY);
                cIndex++;
            }

            lastTick = tick;
        }

        /*
         * Render timeline
         */

        long period = end - start;
        long step = (long) Math.pow(10, Math.floor(Math.log10(period)) - 1);

        g.setColor(Color.BLACK);

        for (long tick = start; tick < end; tick += step) {
            int cY = H_HEIGHT + (int) (D_HEIGHT * (tick - start) / (end - start));
            g.drawLine(10, cY, WIDTH - 10, cY);
            g.drawString(String.format("%d ms", TimeUnit.NANOSECONDS.toMillis(tick - start)), 10, cY - 3);
        }

        /**
         * Render legend
         */
        final int LEG_STEP = 20;

        Set<Color> alreadyPrinted = new HashSet<>();

        int index = 1;
        for (WorkerStatusBL statusBL : WorkerStatusBL.values()) {
            for (WorkerStatusPK statusPK : WorkerStatusPK.values()) {
                for (WorkerStatusJN statusJN : WorkerStatusJN.values()) {
                    int cY = index * LEG_STEP;

                    Color color = selectColor(statusBL, statusPK, statusJN);
                    if (alreadyPrinted.add(color)) {
                        g.setColor(color);
                        g.fillRect(T_WIDTH, cY, LEG_STEP, LEG_STEP);

                        g.setColor(Color.BLACK);
                        g.drawString(selectTextLong(statusBL, statusPK, statusJN), T_WIDTH + LEG_STEP + 5, cY + LEG_STEP - 5);

                        index++;
                    }
                }
            }
        }

        g.drawString("Thread timelines:", T_WIDTH, H_HEIGHT -5 );
        g.drawString("State distribution:", T_WIDTH + W_WIDTH + P_WIDTH, H_HEIGHT - 5);

        ImageIO.write(image, "png", new File(TRACE_GRAPH));
    }

    private Color selectColor(WorkerStatusBL blStatus, WorkerStatusPK pkStatus, WorkerStatusJN jnStatus) {
        switch (blStatus) {
            case IDLE:
                switch (pkStatus) {
                    case ACTIVE:
                        return Color.YELLOW;
                    case PARKED:
                        return Color.getHSBColor(0f, 0f, 0.9f);
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

    private String selectText(WorkerStatusBL blStatus, WorkerStatusPK pkStatus, WorkerStatusJN jnStatus) {
        switch (blStatus) {
            case IDLE:
                switch (pkStatus) {
                    case ACTIVE:
                        return "--- infra ---";
                    case PARKED:
                        return "";
                }
            case RUNNING:
                switch (jnStatus) {
                    case FREE:
                        switch (pkStatus) {
                            case ACTIVE:
                                return "*** exec ***";
                            case PARKED:
                                return "--- wait ---";
                        }
                    case JOINING:
                        switch (pkStatus) {
                            case ACTIVE:
                                return "*** joining ***";
                            case PARKED:
                                return "--- wait ---";
                        }
                }
        }
        throw new IllegalStateException();
    }

    private String selectTextLong(WorkerStatusBL blStatus, WorkerStatusPK pkStatus, WorkerStatusJN jnStatus) {
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

    private void computeStats() {

        Map<Worker, Long> parkedTime = new HashMap<>();
        Map<Worker, Long> parkedSince = new HashMap<>();

        for (Event e : events) {
            switch (e.eventType) {
                case PARK:
                    parkedSince.put(e.worker, e.time);
                    break;

                case UNPARK:
                    Long since = parkedSince.remove(e.worker);
                    if (since != null) {
                        Long ex = parkedTime.get(e.worker);
                        if (ex == null) {
                            ex = 0L;
                        }
                        ex += (e.time - since);
                        parkedTime.put(e.worker, ex);
                    }
                    break;
            }
        }

        PrintWriter pw = new PrintWriter(System.out);
        for (Worker w : workers) {
            Long parked = parkedTime.get(w);
            pw.printf("%20s: time parked = %2d ms\n", w.id, (parked != null) ? TimeUnit.NANOSECONDS.toMillis(parked) : 0);
        }

        pw.close();
    }

    private void renderText() throws FileNotFoundException {
        System.out.println("Rendering text to " + TRACE_TEXT);

        PrintWriter pw = new PrintWriter(new File(TRACE_TEXT));

        pw.println("Total events: "  + events.size());

        long baseTime = events.iterator().next().time;

        pw.format("%10s", "Time, ms");
        for (Worker w : workers) {
            pw.format("%20s", w.id);
        }
        pw.println();

        for (Event e : events) {
            pw.format("%10d", TimeUnit.NANOSECONDS.toMillis(e.time - baseTime));
//            pw.format("%10d", e.time);

            for (Worker w : workers) {
                if (w.equals(e.worker)) {
                    pw.format("%20s", e.eventType + "(" + e.taskHC + ")");
                } else {
                    WorkerStatusBL statusBL = blTimelines.get(w).getStatus(e.time);
                    WorkerStatusPK statusPK = pkTimelines.get(w).getStatus(e.time);
                    WorkerStatusJN statusJN = jnTimelines.get(w).getStatus(e.time);

                    pw.format("%20s", selectText(statusBL, statusPK, statusJN));
                }
            }
            pw.println();
        }
        pw.close();
    }

    private void computeWorkerStatus() {
        long baseTime = events.iterator().next().time - 1;
        for (Worker w : workers) {
            Timeline<WorkerStatusBL> vBL = new Timeline<>();
            Timeline<WorkerStatusPK> vPK = new Timeline<>();
            Timeline<WorkerStatusJN> vJN = new Timeline<>();
            blTimelines.put(w, vBL);
            pkTimelines.put(w, vPK);
            jnTimelines.put(w, vJN);
            vBL.add(baseTime, WorkerStatusBL.IDLE);
            vPK.add(baseTime, WorkerStatusPK.PARKED);
            vJN.add(baseTime, WorkerStatusJN.FREE);
        }

        Multiset<Worker> execDepth = new Multiset<>();
        Multiset<Worker> jnDepth = new Multiset<>();

        for (Event e : events) {
            for (Worker w : workers) {
                if (w.equals(e.worker)) {

                    switch (e.eventType) {
                        case EXEC:
                            execDepth.add(w);
                            blTimelines.get(w).add(e.time, WorkerStatusBL.RUNNING);
                            break;

                        case EXECED:
                            execDepth.remove(w, 1);
                            if (!execDepth.contains(w)) {
                                blTimelines.get(w).add(e.time, WorkerStatusBL.IDLE);
                            }
                            break;

                        case PARK:
                            pkTimelines.get(w).add(e.time, WorkerStatusPK.PARKED);
                            break;

                        case UNPARK:
                            pkTimelines.get(w).add(e.time, WorkerStatusPK.ACTIVE);
                            break;

                        case JOIN:
                            jnDepth.add(w);
                            jnTimelines.get(w).add(e.time, WorkerStatusJN.JOINING);
                            break;

                        case JOINED:
                            jnDepth.remove(w, 1);
                            if (!jnDepth.contains(w)) {
                                jnTimelines.get(w).add(e.time, WorkerStatusJN.FREE);
                            }
                            break;
                    }
                }
            }
        }
    }

    /**
     * Business logic status
     */
    public enum WorkerStatusBL {
        /**
         * Actively executing business logic tasks
         */
        RUNNING,

        /**
         * Not executing business logic tasks
         */
        IDLE,
    }

    /**
     * Thread status
     */
    public enum WorkerStatusPK {
        /**
         * Thread is parked
         */
        PARKED,

        /**
         * Thread is active
         */
        ACTIVE,
    }

    /**
     * Join/helping status
     */
    public enum WorkerStatusJN {
        /**
         * Executing its own tasks
         */
        FREE,

        /**
         * Helping to execute other tasks
         */
        JOINING,
    }

}
