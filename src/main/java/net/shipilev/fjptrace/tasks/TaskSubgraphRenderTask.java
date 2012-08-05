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
import net.shipilev.fjptrace.Selectors;
import net.shipilev.fjptrace.TaskSubgraphs;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;

public class TaskSubgraphRenderTask extends AbstractGraphTask {
    private final TaskSubgraphs data;
    private final Map<Integer, Color> colors;

    public TaskSubgraphRenderTask(Options opts, Events events, TaskSubgraphs data) {
        super(opts, events, "Task subgraphs", opts.getTargetPrefix() + "-subgraphs.png");
        this.data = data;

        this.colors = new HashMap<>();

        assert (data != null);

        // generate distinguished colors
        Random random = new Random(1);
        for (Integer id : data.getIds()) {
            Color color = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            colors.put(id, color);
        }
    }

    @Override
    protected Color getColor(long tick, long worker) {
        Integer id = data.get(tick, worker);
        if (id != null) {
            if (id != TaskSubgraphs.NO_ID) {
                return colors.get(id);
            } else {
                return Selectors.COLOR_DARK_GRAY;
            }
        } else {
            return Color.BLACK;
        }
    }

    @Override
    protected SortedSet<Long> getTicks() {
        return data.getTimes();
    }

    @Override
    protected void renderLegend(Graphics2D g) {
        g.drawString("Subtasks for the same external task are colored the same.", T_WIDTH, 20);
    }
}
