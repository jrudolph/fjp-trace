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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import sun.misc.Unsafe;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
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
import java.util.zip.GZIPOutputStream;

public class Main {

    private static final int HEIGHT = Integer.getInteger("height", 2000);
    private static final int WIDTH = Integer.getInteger("width", 1000);
    private static final int OFFSET = Integer.getInteger("offset", 0);
    private static final int LIMIT = Integer.getInteger("limit", Integer.MAX_VALUE);
    private static final String TRACE_TEXT = System.getProperty("trace.text", "trace.log.gz");
    private static final String TRACE_GRAPH = System.getProperty("trace.graph", "trace.png");

    private final static Unsafe U;
    private final static long BBASE;

    private static final Comparator<Color> COLOR_COMPARATOR = new Comparator<Color>() {
        @Override
        public int compare(Color o1, Color o2) {
            return Integer.compare(o1.getRGB(), o2.getRGB());
        }
    };

    private final Events events = new Events();

    private final Map<Long,Timeline<WorkerStatusBL>> blTimelines = new HashMap<>();
    private final Map<Long,Timeline<WorkerStatusPK>> pkTimelines = new HashMap<>();
    private final Map<Long,Timeline<WorkerStatusJN>> jnTimelines = new HashMap<>();
    private final PairedList selfDurations = new PairedList();
    private final PairedList execDurations = new PairedList();

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
        read(filename);

        computeWorkerStatus();
        computeTaskStatus();

        renderTaskStats();
        renderGraph();
        renderText();
    }

    private void read(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));

        byte[] buffer = new byte[22];

        long basetime = Long.MAX_VALUE;
        int count = 0;
        while (is.read(buffer) == 22) {

            count++;
            long time = U.getLong(buffer, BBASE + 0);
            int eventOrd = U.getShort(buffer, BBASE + 8);
            int taskHC = U.getInt(buffer, BBASE + 10);
            long threadID = U.getLong(buffer, BBASE + 14);

            basetime = Math.min(basetime, time);

            if (count < OFFSET) {
                continue;
            }

            if (count - LIMIT > OFFSET) {
                break;
            }

            Event event = new Event(time, EventType.values()[eventOrd], threadID, taskHC);
            if (!events.add(event)) {
                throw new IllegalStateException("Duplicate event: " + event);
            }
        }
        is.close();

        events.setBasetime(basetime);
        events.seal();
    }

    private void renderGraph() throws IOException {
        System.out.println("Rendering graph to " + TRACE_GRAPH);


        final int H_HEIGHT = (int) (200);
        final int D_HEIGHT = (int) (HEIGHT - H_HEIGHT);

        final int T_WIDTH = (int) (WIDTH * 0.1);
        final int W_WIDTH = (int) (WIDTH * 0.65);
        final int P_WIDTH = (int) (WIDTH * 0.05);
        final int D_WIDTH = (int) (WIDTH * 0.2);

        final int W_STEP = W_WIDTH / events.getWorkers().size();
        final int D_STEP = D_WIDTH / events.getWorkers().size();

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
        for (Timeline t : jnTimelines.values()) {
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

        List<Color> colors = new ArrayList<>();

        long lastTick = events.getStart();
        for (long tick : times) {

            int cY = H_HEIGHT + (int) (D_HEIGHT * (tick - events.getStart()) / (events.getEnd() - events.getStart()));
            int lY = H_HEIGHT + (int) (D_HEIGHT * (lastTick - events.getStart()) / (events.getEnd() - events.getStart()));

            // performance: skip rendering over and over again
            if (cY == lY) {
                lastTick = tick;
                continue;
            }

            colors.clear();

            int wIndex = 0;
            for (long w : events.getWorkers()) {
                WorkerStatusBL blStatus = blTimelines.get(w).getStatus(tick);
                WorkerStatusPK pkStatus = pkTimelines.get(w).getStatus(tick);
                WorkerStatusJN jnStatus = jnTimelines.get(w).getStatus(tick);

                Color color = Selectors.selectColor(blStatus, pkStatus, jnStatus);
                colors.add(color);

                g.setColor(color);

                int cX = T_WIDTH + wIndex * W_STEP;
                g.fillRect(cX, lY, W_STEP, cY - lY);

                wIndex++;
            }

            Collections.sort(colors, COLOR_COMPARATOR);

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

        long period = events.getEnd() - events.getStart();
        long step = (long) Math.pow(10, Math.floor(Math.log10(period)) - 1);

        g.setColor(Color.BLACK);

        for (long tick = events.getStart(); tick < events.getEnd(); tick += step) {
            int cY = H_HEIGHT + (int) (D_HEIGHT * (tick - events.getStart()) / (events.getEnd() - events.getStart()));
            g.drawLine(10, cY, WIDTH - 10, cY);
            g.drawString(String.format("%d ms", TimeUnit.NANOSECONDS.toMillis(tick)), 10, cY - 3);
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

                    Color color = Selectors.selectColor(statusBL, statusPK, statusJN);
                    if (alreadyPrinted.add(color)) {
                        g.setColor(color);
                        g.fillRect(T_WIDTH, cY, LEG_STEP, LEG_STEP);

                        g.setColor(Color.BLACK);
                        g.drawString(Selectors.selectTextLong(statusBL, statusPK, statusJN), T_WIDTH + LEG_STEP + 5, cY + LEG_STEP - 5);

                        index++;
                    }
                }
            }
        }

        g.drawString("Thread timelines:", T_WIDTH, H_HEIGHT -5 );
        g.drawString("State distribution:", T_WIDTH + W_WIDTH + P_WIDTH, H_HEIGHT - 5);

        ImageIO.write(image, "png", new File(TRACE_GRAPH));
    }

    private void renderText() throws IOException {
        System.out.println("Rendering text to " + TRACE_TEXT);

        PrintWriter pw = new PrintWriter(new GZIPOutputStream(new FileOutputStream(TRACE_TEXT)));

        pw.format("%10s", "Time, ms");
        for (long w : events.getWorkers()) {
            pw.format("%20s", w);
        }
        pw.println();

        for (Event e : events) {
            pw.format("%10d", TimeUnit.NANOSECONDS.toMillis(e.time));
//            pw.format("%10d", e.time);

            for (long w : events.getWorkers()) {
                if (w == e.workerId) {
                    pw.format("%20s", e.eventType + "(" + e.taskHC + ")");
                } else {
                    WorkerStatusBL statusBL = blTimelines.get(w).getStatus(e.time);
                    WorkerStatusPK statusPK = pkTimelines.get(w).getStatus(e.time);
                    WorkerStatusJN statusJN = jnTimelines.get(w).getStatus(e.time);

                    pw.print(Selectors.selectText(statusBL, statusPK, statusJN));
                }
            }
            pw.println();
        }
        pw.close();
    }

    private void renderTaskStats() throws IOException {
        System.out.println("Rendering task stats");

        renderChart(selfDurations.filter(1), "exectime-exclusive.png", "Task execution time (exclusive)", "Time to execute, usec");
        renderChart(execDurations.filter(1), "exectime-inclusive.png", "Task execution times (inclusive, including subtasks)", "Time to execute, usec");
    }

    private void renderChart(PairedList data, String filename, String chartLabel, String yLabel) throws IOException {
        System.err.println("Rendering " + chartLabel + " to " + filename);

        XYSeries series = new XYSeries("");
        for (PairedList.Pair entry : data) {
            long x = TimeUnit.NANOSECONDS.toMillis(entry.getK1());
            long dur = TimeUnit.NANOSECONDS.toMicros(entry.getK2());
            if (dur > 0) {
                series.add(x, dur, false);
            }
        }

        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        final JFreeChart chart = ChartFactory.createXYLineChart(
                "",
                "Run time, msec", yLabel,
                dataset,
                PlotOrientation.HORIZONTAL,
                false, false, false
        );

        chart.setBackgroundPaint(Color.white);
//        chart.getLegend().setPosition(RectangleEdge.BOTTOM);

        final XYPlot plot = chart.getXYPlot();
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setDefaultEntityRadius(3);
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setForegroundAlpha(0.65f);
        plot.setDomainGridlinePaint(Color.gray);
        plot.setRangeGridlinePaint(Color.gray);

        final ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickMarkPaint(Color.black);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setInverted(true);
        domainAxis.setLowerBound(TimeUnit.NANOSECONDS.toMillis(events.getStart()));
        domainAxis.setUpperBound(TimeUnit.NANOSECONDS.toMillis(events.getEnd()));

        final ValueAxis rangeAxis = new LogarithmicAxis(yLabel);
        rangeAxis.setTickMarkPaint(Color.black);
        rangeAxis.setStandardTickUnits(new StandardTickUnitSource());
        plot.setRangeAxis(rangeAxis);

        AxisSpace space = new AxisSpace();
        space.setLeft(50);

        plot.setFixedDomainAxisSpace(space);

        final HistogramDataset histDataSet = new HistogramDataset();
        double[] values = new double[data.getAllY().length];

        int c = 0;
        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;
        for (long l : data.getAllY()) {
            values[c++] = TimeUnit.NANOSECONDS.toMicros(l);
            min = Math.min(min, l);
            max = Math.max(max, l);
        }
        histDataSet.addSeries("H1", Arrays.copyOf(values, c), WIDTH);

        final JFreeChart histChart = ChartFactory.createHistogram(
                chartLabel,
                "", "Samples",
                histDataSet,
                PlotOrientation.VERTICAL,
                false, false, false
        );

        histChart.setBackgroundPaint(Color.white);
//        histChart.getLegend().setPosition(RectangleEdge.BOTTOM);

        rangeAxis.setAutoRange(false);
        histChart.getXYPlot().setDomainAxis(rangeAxis);
        histChart.getXYPlot().setFixedRangeAxisSpace(space);

        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bi.createGraphics();

        histChart.draw(graphics, new Rectangle(0, 0, WIDTH, 200));
        chart.draw(graphics, new Rectangle(0, 200, WIDTH, HEIGHT - 200));

        FileOutputStream out = new FileOutputStream(filename);
        ChartUtilities.writeBufferedImageAsPNG(out, bi);
        out.close();
    }


    private void computeTaskStatus() {
        System.out.println("Computing task stats");

        Map<Integer, Long> execTime = new HashMap<>();
        Map<Integer, Long> lastSelfTime = new HashMap<>();

        Map<Long, Integer> currentExec = new HashMap<>();
        Map<Integer, Integer> parentTasks = new HashMap<>();

        Multiset<Integer> timings = new Multiset<>();

        for (Event e : events) {
            switch (e.eventType) {
                case EXEC:
                    Integer currentTask = currentExec.get(e.workerId);

                    if (currentTask != null) {
                        // about to leave parent
                        parentTasks.put(e.taskHC, currentTask);

                        Long start = lastSelfTime.remove(currentTask);
                        if (start == null) {
                            continue;
                        }
                        timings.add(currentTask, e.time - start);
                    }

                    // start executing
                    lastSelfTime.put(e.taskHC, e.time);
                    currentExec.put(e.workerId, e.taskHC);
                    execTime.put(e.taskHC, e.time);

                    break;

                case EXECED:
                    // record worker is free
                    currentExec.remove(e.workerId);

                    // count remaining self time
                    Long s = lastSelfTime.remove(e.taskHC);
                    if (s == null) {
                        continue;
                    }
                    timings.add(e.taskHC, e.time - s);
                    selfDurations.add((e.time - timings.count(e.taskHC) / 2), timings.count(e.taskHC));
                    timings.removeKey(e.taskHC);

                    // count the time
                    Long s1 = execTime.remove(e.taskHC);
                    if (s1 == null) {
                        continue;
                    }
                    execDurations.add((e.time + s1) / 2, e.time - s1);

                    Integer parent = parentTasks.remove(e.taskHC);
                    if (parent != null) {
                        // getting back to parent
                        lastSelfTime.put(parent, e.time);
                        currentExec.put(e.workerId, parent);
                    }

                    break;
            }
        }
    }

    private void computeWorkerStatus() {
        System.out.println("Computing worker status");

        int execDepth = 0;
        int jnDepth = 0;

        for (long w : events.getWorkers()) {

            Timeline<WorkerStatusBL> vBL = new Timeline<>();
            Timeline<WorkerStatusPK> vPK = new Timeline<>();
            Timeline<WorkerStatusJN> vJN = new Timeline<>();
            blTimelines.put(w, vBL);
            pkTimelines.put(w, vPK);
            jnTimelines.put(w, vJN);
            vBL.add(-1, WorkerStatusBL.IDLE);
            vPK.add(-1, WorkerStatusPK.PARKED);
            vJN.add(-1, WorkerStatusJN.FREE);

            for (Event e : events) {
                if (w != e.workerId) {
                    continue;
                }

                switch (e.eventType) {
                    case EXEC:
                        execDepth++;
                        vBL.add(e.time, WorkerStatusBL.RUNNING);
                        break;

                    case EXECED:
                        execDepth--;
                        if (execDepth == 0) {
                            vBL.add(e.time, WorkerStatusBL.IDLE);
                        }
                        break;

                    case PARK:
                        vPK.add(e.time, WorkerStatusPK.PARKED);
                        break;

                    case UNPARK:
                        vPK.add(e.time, WorkerStatusPK.ACTIVE);
                        break;

                    case JOIN:
                        jnDepth++;
                        vJN.add(e.time, WorkerStatusJN.JOINING);
                        break;

                    case JOINED:
                        jnDepth--;
                        if (jnDepth == 0) {
                            vJN.add(e.time, WorkerStatusJN.FREE);
                        }
                        break;
                }
            }
        }
    }

}
