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
import net.shipilev.fjptrace.QueueStatus;

import java.awt.*;
import java.util.SortedSet;

public class RenderWorkerQueueTask extends AbstractGraphTask {

    private final QueueStatus queueStatus;

    public RenderWorkerQueueTask(Options opts, Events events, QueueStatus queueStatus) {
        super(opts, events, "Queue stats", opts.getTargetPrefix() + "-workerQueue.png");
        this.queueStatus = queueStatus;
    }

    @Override
    protected Color getColor(long tick, long worker) {
        long depth = Math.max(0, queueStatus.getDepth(tick, worker));
        long maxDepth = queueStatus.getMaxCount();

        return new Color(0, 1.0f * depth / maxDepth, 0);
    }

    @Override
    protected SortedSet<Long> getTicks() {
        return queueStatus.getTimes();
    }

    @Override
    protected void renderLegend(Graphics2D g) {
        final int LEG_STEP = 20;

        g.drawString("Elements in worker queue: ", T_WIDTH, LEG_STEP);

        long maxDepth = Math.max(queueStatus.getMaxCount(), 1);
        int STEP = (int) Math.max(1, W_WIDTH / maxDepth);
        for (int d = 0; d < maxDepth; d++) {
            Color color = new Color(0, 1.0f * d / maxDepth, 0);
            g.setColor(color);
            g.fillRect(T_WIDTH + d * STEP, LEG_STEP * 2, STEP, LEG_STEP);
        }

        for (int d = 0; d < maxDepth; d += Math.max(1, maxDepth / 10)) {
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(d), Math.round(T_WIDTH + (d + 0.5) * STEP), LEG_STEP * 4);
        }
    }

}
