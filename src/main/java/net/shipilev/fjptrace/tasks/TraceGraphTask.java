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

public class TraceGraphTask extends AbstractGraphTask {

    private static final String TRACE_GRAPH = System.getProperty("trace.graph", "trace.png");

    private final WorkerStatus workerStatus;

    public TraceGraphTask(Events events, WorkerStatus workerStatus) {
        super(events, "Rendering graph", TRACE_GRAPH);
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
