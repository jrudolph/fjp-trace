package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.QueueStatus;
import net.shipilev.fjptrace.WorkerStatusHolder;
import net.shipilev.fjptrace.util.Multiset;
import net.shipilev.fjptrace.Selectors;
import net.shipilev.fjptrace.WorkerStatus;
import net.shipilev.fjptrace.WorkerStatusBL;
import net.shipilev.fjptrace.WorkerStatusJN;
import net.shipilev.fjptrace.WorkerStatusPK;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class TraceGraphTask extends LoggedRecursiveAction {
    private static final int HEIGHT = Integer.getInteger("height", 2000);
    private static final int WIDTH = Integer.getInteger("width", 1000);
    private static final String TRACE_GRAPH = System.getProperty("trace.graph", "trace.png");

    private static final Comparator<Color> COLOR_COMPARATOR = new Comparator<Color>() {
        @Override
        public int compare(Color o1, Color o2) {
            return Integer.compare(o1.getRGB(), o2.getRGB());
        }
    };

    private final Events events;
    private final WorkerStatus workerStatus;
    private final QueueStatus queueStatus;

    public TraceGraphTask(Events events, WorkerStatus workerStatus, QueueStatus queueStatus) {
        super("Rendering graph to " + TRACE_GRAPH);
        this.events = events;
        this.workerStatus = workerStatus;
        this.queueStatus = queueStatus;
    }

    @Override
    public void doWork() throws Exception {
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
        SortedSet<Long> times = workerStatus.getTimes();

        /*
          Render it!
        */

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setBackground(Color.WHITE);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        Map<Long, Multiset<Color>> workerColors = new TreeMap<>();

        for (int yTick = 0; yTick < D_HEIGHT; yTick++) {
            if ((yTick & 0xF) == 0) {
                reportProgress(yTick * 1.0 / D_HEIGHT);
            }

            int cY = H_HEIGHT + yTick;

            long loTick = (long) ((1.0 * (yTick-1) / D_HEIGHT) * (events.getEnd() - events.getStart()) + events.getStart());
            long hiTick = (long) ((1.0 * (yTick) / D_HEIGHT) * (events.getEnd() - events.getStart()) + events.getStart());

            Collection<Long> slice = times.tailSet(loTick).headSet(hiTick);

            if (slice.isEmpty()) {
                SortedSet<Long> head = times.headSet(hiTick);
                if (head.isEmpty()) {
                    slice = Collections.emptyList();
                } else {
                    slice = Collections.singleton(head.last());
                }
            }

            {
                for (long tick : slice) {
                    for (long w : events.getWorkers()) {
                        WorkerStatusHolder status = workerStatus.getStatus(w, tick);
                        Color color = Selectors.selectColor(status.blStatus, status.pkStatus, status.jnStatus);

                        Multiset<Color> ms = workerColors.get(w);
                        if (ms == null) {
                            ms = new Multiset<>();
                            workerColors.put(w, ms);
                        }
                        ms.add(color);
                    }
                }

            }

            // render predominant color
            List<Color> mColors = new ArrayList<>();
            for (long w : workerColors.keySet()) {
//                mColors.add(averageColor(workerColors.get(w)));
                mColors.add(workerColors.get(w).getMostFrequent());
            }

            workerColors.clear();

            int wIndex = 0;
            for (Color color : mColors) {
                g.setColor(color);
                g.drawLine(T_WIDTH + wIndex * W_STEP, cY, T_WIDTH + (wIndex + 1)* W_STEP, cY);
                wIndex++;
            }

            Collections.sort(mColors, COLOR_COMPARATOR);

            int cIndex = 0;
            for (Color c : mColors) {
                g.setColor(c);
                g.drawLine(T_WIDTH + W_WIDTH + P_WIDTH + cIndex * D_STEP, cY, T_WIDTH + W_WIDTH + P_WIDTH + (cIndex + 1) * D_STEP, cY);
                cIndex++;
            }

        }

        /**
         * Render external submissions
         */
//        for (long tick : queueStatus.getTimes()) {
//            int cY = H_HEIGHT + (int) (D_HEIGHT * (tick - events.getStart()) / (events.getEnd() - events.getStart()));
//            g.drawLine(T_WIDTH, cY, T_WIDTH + W_WIDTH, cY);
//        }

        /*
         * Render timeline
         */

        long period = events.getEnd() - events.getStart();
        long step = (long) Math.pow(10, Math.floor(Math.log10(period)) - 1);

        g.setColor(Color.BLACK);

        for (long tick = events.getStart(); tick < events.getEnd(); tick += step) {
            int cY = H_HEIGHT + (int) (D_HEIGHT * (tick - events.getStart()) / (events.getEnd() - events.getStart()));
            g.drawLine(10, cY, T_WIDTH, cY);
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

        g.drawString("Thread timelines:", T_WIDTH, H_HEIGHT - 5);
        g.drawString("State distribution:", T_WIDTH + W_WIDTH + P_WIDTH, H_HEIGHT - 5);

        ImageIO.write(image, "png", new File(TRACE_GRAPH));
    }

}