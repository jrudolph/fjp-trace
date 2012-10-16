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
import net.shipilev.fjptrace.Selectors;
import net.shipilev.fjptrace.Task;
import net.shipilev.fjptrace.TaskStatus;
import net.shipilev.fjptrace.WorkerStatusBL;
import net.shipilev.fjptrace.WorkerStatusJN;
import net.shipilev.fjptrace.WorkerStatusPK;
import net.shipilev.fjptrace.util.Multimap;
import net.shipilev.fjptrace.util.PairedList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import sun.font.Type1Font;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class PrintTaskTreesTask extends LoggedRecursiveAction {

    private final TaskStatus subgraphs;
    private final String fileNamePng;
    private final Events events;
    private final long fromTime;
    private final long toTime;
    private final int width;
    private final int height;

    private static final int PAD_LEFT = 150;
    private static final int PAD_RIGHT = 50;
    private static final int PAD_TOP = 200;
    private static final int PAD_BOTTOM = 50;

    // transient
    private long minTime;
    private long maxTime;
    private Map<Long,Integer> workerId;

    public PrintTaskTreesTask(Options opts, Events events, TaskStatus subgraphs) {
        super("Print task subtrees");
        this.fileNamePng = opts.getTargetPrefix() + "-subtrees.png";
        this.fromTime = opts.getFromTime();
        this.toTime = opts.getToTime();
        this.width = opts.getWidth();
        this.height = opts.getHeight();
        this.events = events;
        this.subgraphs = subgraphs;
    }

    @Override
    public void doWork() throws Exception {
        // walk the trees

        // only record the events for the interesting region
        for (Event e : events) {
            if (e.time < fromTime) continue;
            if (e.time > toTime) break;
            subgraphs.recordEvent(e);
        }

        // only care about the parents in the interesting region
        Collection<Task> interestingParents = new ArrayList<>();
        for (Task t : subgraphs.getParents()) {
            if (fromTime < t.getTime() && t.getTime() < toTime) {
                interestingParents.add(t);
            }
        }

        List<Event> allEvents = new ArrayList<>();
        Multimap<Task, Event> subEvents = new Multimap<>();

        for (Task t : interestingParents) {

            // compute transitive closure

            Set<Task> visited = new HashSet<>();
            List<Task> prev = new ArrayList<>();
            List<Task> cur = new ArrayList<>();

            List<Event> events = new ArrayList<>();

            cur.add(t);
            while (visited.addAll(cur)) {
                prev.clear();
                prev.addAll(cur);
                cur.clear();

                for (Task c : prev) {
                    Collection<Task> children = c.getChildren();
                    if (!children.isEmpty()) {
                        cur.addAll(children);
                    }
                    events.addAll(c.getEvents());
                }

            }

            allEvents.addAll(events);
            subEvents.putAll(t, events);
        }

        // emit graph info

        Collections.sort(allEvents, new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
                return Long.compare(o1.time, o2.time);
            }
        });

        // enumerate workers
        Set<Long> workers = new HashSet<>();
        workerId = new HashMap<>();
        {
            int id = 0;
            for (Event e : allEvents) {
                if (workers.add(e.workerId)) {
                    workerId.put(e.workerId, id++);
                }
            }
        }

        // figure out min and max time
        minTime = Long.MAX_VALUE;
        maxTime = Long.MIN_VALUE;
        for (Event e : allEvents) {
            minTime = Math.min(minTime, e.time);
            maxTime = Math.max(maxTime, e.time);
        }

        render(allEvents, subEvents);
    }

    private Point map(Event e) {
        return new Point(
                PAD_LEFT + ((width - (PAD_RIGHT + PAD_LEFT)) * workerId.get(e.workerId) / workerId.size()),
                PAD_TOP + (int)((height - (PAD_TOP + PAD_BOTTOM)) * (e.time - minTime) / (maxTime - minTime))
                );
    }

    private void render(Collection<Event> events, Multimap<Task, Event> subEvents) throws IOException {

        // split tasks
        Multimap<Integer, Event> tasks = new Multimap<>();
        for (Event e : events) {
            tasks.put(e.taskTag, e);
        }

        // render graph: prepare canvas
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setBackground(Color.WHITE);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // render graph: edges
        // Inter-thread edges:
        //   SUBMIT -> EXEC
        //     FORK -> EXEC
        //     EXECED -> JOINED
        for (Integer id : tasks.keySet()) {
            List<Event> list = tasks.get(id);

            Multimap<EventType, Event> byType = new Multimap<>();
            for (Event e : list) {
                byType.put(e.eventType, e);
            }

            for (Pair<Event, Event> pair : product(byType.get(EventType.SUBMIT), byType.get(EventType.EXEC))) {
                Point p1 = map(pair.t1);
                Point p2 = map(pair.t2);
                g.setColor(Color.BLUE);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            for (Pair<Event, Event> pair : product(byType.get(EventType.FORK), byType.get(EventType.EXEC))) {
                Point p1 = map(pair.t1);
                Point p2 = map(pair.t2);
                g.setColor(Color.GREEN);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            for (Pair<Event, Event> pair : product(byType.get(EventType.EXECUTED), byType.get(EventType.JOINED))) {
                Point p1 = map(pair.t1);
                Point p2 = map(pair.t2);
                g.setColor(Color.RED);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            for (Pair<Event, Event> pair : product(byType.get(EventType.EXEC), byType.get(EventType.EXECUTED))) {
                Point p1 = map(pair.t1);
                Point p2 = map(pair.t2);
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // render graph: nodes
        for (Event e : events) {
            Point p = map(e);
            g.setColor(Color.BLACK);
            g.fillRect(p.x - 2, p.y - 2, 4, 4);
        }

        // time scale
        long period = maxTime - minTime;
        long step = (long) Math.pow(10, Math.floor(Math.log10(period)) - 1);
        long start = (minTime / step) * step;

        g.setColor(Color.BLACK);

        for (long tick = start; tick < maxTime; tick += step) {
            int cY = PAD_TOP + (int) ((height - (PAD_TOP + PAD_BOTTOM)) * (tick - minTime) / (maxTime - minTime));
            g.drawLine(10, cY, width - (PAD_RIGHT), cY);
            g.drawString(String.format("%d us", TimeUnit.NANOSECONDS.toMicros(tick)), 10, cY - 3);
        }

        // legend
        final int LEG_STEP = 20;

        g.setColor(Color.BLUE);         g.fillRect(10 + PAD_LEFT, 10 + 0*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.GREEN);        g.fillRect(10 + PAD_LEFT, 10 + 1*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.RED);          g.fillRect(10 + PAD_LEFT, 10 + 2*LEG_STEP, LEG_STEP, LEG_STEP);
        g.setColor(Color.LIGHT_GRAY);   g.fillRect(10 + PAD_LEFT, 10 + 3*LEG_STEP, LEG_STEP, LEG_STEP);

        g.setColor(Color.BLACK);
        g.drawString("SUBMIT -> EXEC edge for the same task",       10 + PAD_LEFT + LEG_STEP + 3, 10 + 0*LEG_STEP + LEG_STEP - 5);
        g.drawString("FORK -> EXEC edge for the same task",         10 + PAD_LEFT + LEG_STEP + 3, 10 + 1*LEG_STEP + LEG_STEP - 5);
        g.drawString("EXECUTED -> JOINED edge for the same task",   10 + PAD_LEFT + LEG_STEP + 3, 10 + 2*LEG_STEP + LEG_STEP - 5);
        g.drawString("EXEC -> EXECUTED edge for the same task",     10 + PAD_LEFT + LEG_STEP + 3, 10 + 3*LEG_STEP + LEG_STEP - 5);


        ImageIO.write(image, "png", new File(fileNamePng));
    }

    private <T1, T2> Collection<Pair<T1, T2>> product(Collection<T1> list1, Collection<T2> list2) {
        List<Pair<T1, T2>> result = new ArrayList<Pair<T1, T2>>();
        for (T1 e1 : list1) {
            for (T2 e2 : list2) {
                result.add(new Pair<T1, T2>(e1, e2));
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
