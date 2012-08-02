package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.QueueStatus;

import java.awt.*;
import java.util.SortedSet;

public class QueueGraphTask extends AbstractGraphTask {

    private static final String TRACE_GRAPH = System.getProperty("queue.graph", "queue-graph.png");

    private final QueueStatus queueStatus;

    public QueueGraphTask(Events events, QueueStatus queueStatus) {
        super(events, "Rendering queue stats", TRACE_GRAPH);
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

        long maxDepth = queueStatus.getMaxCount();
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
