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

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.EventType;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Options;
import net.shipilev.fjptrace.Task;
import net.shipilev.fjptrace.TaskStatus;
import net.shipilev.fjptrace.util.Multimap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PrintTaskTreesTask extends LoggedRecursiveAction {

    private final static boolean DETAIL = Boolean.getBoolean("taskTree.detail");
    private static final Color EXECUTED_COLOR = new Color(200, 200, 200);
    private static final Color COMPLETING_COLOR = new Color(200, 200, 0);

    private final TaskStatus subgraphs;
    private final String fileNamePng;
    private final Events exEvents;
    private final long fromTime;
    private final long toTime;
    private final int width;
    private final int height;

    private static final int PAD_LEFT = 150;
    private static final int PAD_RIGHT = 50;
    private static final int PAD_TOP = 200;
    private static final int PAD_BOTTOM = 50;

    // transient
    private Map<Long,Integer> workerId;

    public PrintTaskTreesTask(Options opts, Events events, TaskStatus subgraphs) {
        super("Print task subtrees");
        this.fileNamePng = opts.getTargetPrefix() + "-subtrees.png";
        this.exEvents = events;
        this.fromTime = Math.max(events.getStart(), opts.getFromTime());
        this.toTime = Math.min(events.getEnd(), opts.getToTime());
        this.width = opts.getWidth();
        this.height = opts.getHeight();
        this.subgraphs = subgraphs;
    }

    @Override
    public void doWork() throws Exception {
        // walk the trees

        List<Event> allEvents = new ArrayList<>();

        // only record the events for the interesting region
        for (Event e : exEvents) {
            if (e.time < fromTime) continue;
            if (e.time > toTime) break;
            subgraphs.recordEvent(e);
            allEvents.add(e);
        }

        Collections.sort(allEvents, new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
                return Long.compare(o1.time, o2.time);
            }
        });

        // enumerate workers
        workerId = new HashMap<>();
        {
            workerId.put(-1L, 0);
            int id = 1;
            for (Long w : exEvents.getWorkers()) {
                workerId.put(w, id++);
            }
        }

        render(allEvents);
    }

    private Point map(Event e) {
        return new Point(
                PAD_LEFT + ((width - (PAD_RIGHT + PAD_LEFT)) * workerId.get(e.workerId) / workerId.size()),
                PAD_TOP + (int)((height - (PAD_TOP + PAD_BOTTOM)) * (e.time - fromTime) / (toTime - fromTime))
                );
    }

    private void render(Collection<Event> allEvents) throws IOException {

        // split tasks
        Multimap<Integer, Event> tasks = new Multimap<>();
        for (Event e : allEvents) {
            tasks.put(e.tag, e);
        }

        // render graph: prepare canvas
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setBackground(Color.WHITE);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // render graph: edges
        // Inter-thread edges:
        renderEdges(tasks, g, EventType.SUBMIT, EventType.EXEC, false, Color.BLUE);
        renderEdges(tasks, g, EventType.FORK, EventType.EXEC, false, Color.GREEN);
        renderEdges(tasks, g, EventType.EXECUTED, EventType.JOINED, false, Color.ORANGE);
        renderEdges(tasks, g, EventType.COMPLETED, EventType.INVOKED, false, Color.ORANGE);
        renderEdges(tasks, g, EventType.COMPLETED, EventType.JOINED, false, Color.ORANGE);
        renderEdges(tasks, g, EventType.COMPLETED, EventType.WAITED, false, Color.ORANGE);
        renderEdges(tasks, g, EventType.EXEC, EventType.EXECUTED, true, EXECUTED_COLOR);
        renderEdges(tasks, g, EventType.COMPLETING, EventType.COMPLETED, true, COMPLETING_COLOR);

        // render graph: edges
        // Inter-thread nodes:
        //   UNPARK -> UNPARKED
        Map<Integer, Event> unparkRequests = new HashMap<>();
        for (Event e : allEvents) {
            if (e.eventType == EventType.UNPARK) {
                unparkRequests.put(e.tag, e);
            }

            if (e.eventType == EventType.UNPARKED) {
                Event unparkReq = unparkRequests.get(e.tag);
                if (unparkReq != null) {
                    Point p1 = map(unparkReq);
                    Point p2 = map(e);
                    g.setColor(Color.RED);
                    g.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // render graph: nodes
        for (Event e : allEvents) {
            Point p = map(e);
            g.setColor(Color.BLACK);
            g.fillRect(p.x - 3, p.y - 3, 6, 6);
            g.setColor(getEventColor(e));
            g.fillRect(p.x - 2, p.y - 2, 4, 4);
            if (DETAIL) {
                g.drawString(e.shortID(), p.x + 2, p.y + 5);
            }
        }

        // time scale
        long period = toTime - fromTime;
        long step = (long) Math.pow(10, Math.floor(Math.log10(period) - 1));
        long start = (fromTime / step) * step;

        g.setColor(Color.BLACK);

        for (long tick = start; tick < toTime; tick += step) {
            int cY = PAD_TOP + (int) ((height - (PAD_TOP + PAD_BOTTOM)) * (tick - fromTime) / (toTime - fromTime));
            g.drawLine(10, cY, width - (PAD_RIGHT), cY);
            g.drawString(String.format("%d us", TimeUnit.NANOSECONDS.toMicros(tick)), 10, cY - 3);
        }

        // legend
        final int LEG_STEP = 20;

        g.setColor(Color.BLUE);         g.fillRect(10 + PAD_LEFT, 10 + 0*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.GREEN);        g.fillRect(10 + PAD_LEFT, 10 + 1*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.ORANGE);       g.fillRect(10 + PAD_LEFT, 10 + 2*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.ORANGE);       g.fillRect(10 + PAD_LEFT, 10 + 3*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.ORANGE);       g.fillRect(10 + PAD_LEFT, 10 + 4*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.RED);          g.fillRect(10 + PAD_LEFT, 10 + 5*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.LIGHT_GRAY);   g.fillRect(10 + PAD_LEFT, 10 + 6*LEG_STEP, LEG_STEP, LEG_STEP);

        g.setColor(Color.BLACK);
        g.drawString("SUBMIT -> EXEC edge for the same task",       10 + PAD_LEFT + LEG_STEP + 3, 10 + 0*LEG_STEP + LEG_STEP - 5);
        g.drawString("FORK -> EXEC edge for the same task",         10 + PAD_LEFT + LEG_STEP + 3, 10 + 1*LEG_STEP + LEG_STEP - 5);
        g.drawString("EXECUTED -> JOINED edge for the same task",   10 + PAD_LEFT + LEG_STEP + 3, 10 + 2*LEG_STEP + LEG_STEP - 5);
        g.drawString("COMPLETED -> JOINED edge for the same task",  10 + PAD_LEFT + LEG_STEP + 3, 10 + 3*LEG_STEP + LEG_STEP - 5);
        g.drawString("COMPLETED -> INVOKED edge for the same task", 10 + PAD_LEFT + LEG_STEP + 3, 10 + 4*LEG_STEP + LEG_STEP - 5);
        g.drawString("UNPARK -> UNPARKED edge for the same thread", 10 + PAD_LEFT + LEG_STEP + 3, 10 + 5*LEG_STEP + LEG_STEP - 5);
        g.drawString("EXEC -> EXECUTED edge for the same task",     10 + PAD_LEFT + LEG_STEP + 3, 10 + 6*LEG_STEP + LEG_STEP - 5);


        ImageIO.write(image, "png", new File(fileNamePng));
    }

    public void renderEdges(Multimap<Integer, Event> tasks, Graphics g, EventType type1, EventType type2, boolean sameThread, Color color) {
        for (Integer id : tasks.keySet()) {
            List<Event> list = tasks.get(id);

            Multimap<EventType, Event> byType = new Multimap<>();
            for (Event e : list) {
                byType.put(e.eventType, e);
            }

            for (Pair<Event, Event> pair : product(byType.get(type1), byType.get(type2), sameThread)) {
                Point p1 = map(pair.t1);
                Point p2 = map(pair.t2);
                g.setColor(color);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    private Color getEventColor(Event e) {
        switch (e.eventType) {
            case FORK:
                return Color.GREEN;
            case SUBMIT:
                return Color.BLUE;
            case PARK:
                return Color.RED;
            case EXEC:
            case EXECUTED:
                return EXECUTED_COLOR;
            case COMPLETING:
            case COMPLETED:
                return Color.YELLOW;
            default:
                return Color.BLACK;
        }
    }

    private Collection<Pair<Event, Event>> product(Collection<Event> list1, Collection<Event> list2, boolean sameThread) {
        List<Pair<Event, Event>> result = new ArrayList<>();
        for (Event e1 : list1) {
            for (Event e2 : list2) {
                if (e1.workerId != e2.workerId || sameThread) {
                    result.add(new Pair<Event, Event>(e1, e2));
                }
            }
        }
        return result;
    }

    private static class Pair<T1, T2> {
        private final T1 t1;
        private final T2 t2;

        public Pair(T1 t1, T2 t2) {
            this.t1 = t1;
            this.t2 = t2;
        }
    }

}
