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
import net.shipilev.fjptrace.WorkerStatus;
import net.shipilev.fjptrace.WorkerStatusBL;
import net.shipilev.fjptrace.WorkerStatusHolder;
import net.shipilev.fjptrace.WorkerStatusJN;
import net.shipilev.fjptrace.WorkerStatusPK;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

public class TraceGraphTask extends AbstractGraphTask {

    private final WorkerStatus workerStatus;

    public TraceGraphTask(Options opts, Events events, WorkerStatus workerStatus) {
        super(opts, events, "Task graph", opts.getTargetPrefix() + "-trace.png");
        this.workerStatus = workerStatus;
    }

    @Override
    protected Color getColor(long tick, long worker) {
        WorkerStatusHolder status = workerStatus.getStatus(worker, tick);
        return Selectors.selectColor(status.blStatus, status.pkStatus, status.jnStatus);
    }

    @Override
    protected SortedSet<Long> getTicks() {
        return workerStatus.getTimes();
    }

    @Override
    protected void renderLegend(Graphics2D g) {
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
    }

}
