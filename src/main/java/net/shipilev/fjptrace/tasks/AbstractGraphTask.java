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

package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Options;
import net.shipilev.fjptrace.util.Multiset;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractGraphTask extends LoggedRecursiveAction {
    private static final Comparator<Color> COLOR_COMPARATOR = new Comparator<Color>() {
        @Override
        public int compare(Color o1, Color o2) {
            return Integer.compare(o1.getRGB(), o2.getRGB());
        }
    };

    private final String outFile;

    private final Events events;
    protected final int HEIGHT;
    protected final int WIDTH;
    protected final int H_HEIGHT;
    protected final int D_HEIGHT;
    protected final int T_WIDTH;
    protected final int W_WIDTH;
    protected final int P_WIDTH;
    protected final int D_WIDTH;
    protected final long fromTime;
    protected final long toTime;

    public AbstractGraphTask(Options opts, Events events, String name, String outFile) {
        super(name);
        this.outFile = outFile;
        this.events = events;
        this.HEIGHT = opts.getHeight();
        this.WIDTH = opts.getWidth();
        this.fromTime = opts.getFromTime();
        this.toTime = opts.getToTime();
        this.H_HEIGHT = 200;
        this.D_HEIGHT = (HEIGHT - H_HEIGHT);
        this.T_WIDTH = (int) (WIDTH * 0.1);
        this.W_WIDTH = (int) (WIDTH * 0.65);
        this.P_WIDTH = (int) (WIDTH * 0.05);
        this.D_WIDTH = (int) (WIDTH * 0.2);
    }

    protected abstract Color getColor(long tick, long worker);
    protected abstract SortedSet<Long> getTicks();
    protected abstract void renderLegend(Graphics2D g);

    @Override
    public void doWork() throws Exception {
        final int W_STEP = W_WIDTH / events.getWorkers().size();
        final int D_STEP = D_WIDTH / events.getWorkers().size();

        /*
          Compute pivot points
        */
        SortedSet<Long> times = getTicks();

        /*
          Render it!
        */

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setBackground(Color.WHITE);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);


        long from = Math.max(fromTime, events.getStart());
        long to = Math.min(toTime, events.getEnd());

        Map<Long, Multiset<Color>> workerColors = new TreeMap<>();

        for (int yTick = 0; yTick < D_HEIGHT; yTick++) {
            if ((yTick & 0xFF) == 0) {
                reportProgress(yTick * 1.0 / D_HEIGHT);
            }

            int cY = H_HEIGHT + yTick;

            long loTick = (long) ((1.0 * (yTick-1) / D_HEIGHT) * (to - from) + from);
            long hiTick = (long) ((1.0 * (yTick) / D_HEIGHT) * (to - from) + from);

            Collection<Long> slice = times.tailSet(loTick).headSet(hiTick);

            if (slice.isEmpty()) {
                SortedSet<Long> head = times.headSet(hiTick);
                if (head.isEmpty()) {
                    slice = Collections.emptyList();
                } else {
                    slice = Collections.singleton(head.last());
                }
            }

            long lastTick = loTick;
            for (long tick : slice) {
                for (long w : events.getWorkers()) {
                    Color color = getColor(tick, w);

                    Multiset<Color> ms = workerColors.get(w);
                    if (ms == null) {
                        ms = new Multiset<>();
                        workerColors.put(w, ms);
                    }
                    ms.add(color, tick - lastTick);
                }
                lastTick = tick;
            }

            // render predominant color
            List<Color> mColors = new ArrayList<>();
            for (long w : workerColors.keySet()) {
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

        /*
         * Render timeline
         */

        long period = to - from;
        long step = (long) Math.pow(10, Math.floor(Math.log10(period)) - 1);

        g.setColor(Color.BLACK);

        for (long tick = from; tick < to; tick += step) {
            int cY = H_HEIGHT + (int) (D_HEIGHT * (tick - from) / (to - from));
            g.drawLine(10, cY, T_WIDTH, cY);
            g.drawString(String.format("%d us", TimeUnit.NANOSECONDS.toMicros(tick)), 10, cY - 3);
        }

        /**
         * Render legend
         */
        renderLegend(g);

        g.setColor(Color.BLACK);
        g.drawString("Thread timelines:", T_WIDTH, H_HEIGHT - 5);
        g.drawString("State distribution:", T_WIDTH + W_WIDTH + P_WIDTH, H_HEIGHT - 5);

        ImageIO.write(image, "png", new File(outFile));
    }

}
