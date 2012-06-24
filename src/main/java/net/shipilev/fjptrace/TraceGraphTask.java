package net.shipilev.fjptrace;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

public class TraceGraphTask extends RecursiveAction {
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

    public TraceGraphTask(Events events, WorkerStatus WorkerStatus) {
        this.events = events;
        workerStatus = WorkerStatus;
    }

    @Override
    public void compute() {
        try {
            call();
        } catch (Exception e) {
            // do nothing
        }
    }

    public void call() throws Exception {
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
        SortedSet<Long> times = workerStatus.getTimes();

        /*
          Render it!
        */

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setBackground(Color.WHITE);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        java.util.List<Color> colors = new ArrayList<>();

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
                WorkerStatusBL statusBL = workerStatus.getBLStatus(w, tick);
                WorkerStatusPK statusPK = workerStatus.getPKStatus(w, tick);
                WorkerStatusJN statusJN = workerStatus.getJNStatus(w, tick);

                Color color = Selectors.selectColor(statusBL, statusPK, statusJN);
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
                g.fillRect(T_WIDTH + W_WIDTH + P_WIDTH + cIndex * D_STEP, lY, D_STEP, cY - lY);
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

        g.drawString("Thread timelines:", T_WIDTH, H_HEIGHT - 5);
        g.drawString("State distribution:", T_WIDTH + W_WIDTH + P_WIDTH, H_HEIGHT - 5);

        ImageIO.write(image, "png", new File(TRACE_GRAPH));
    }

}
