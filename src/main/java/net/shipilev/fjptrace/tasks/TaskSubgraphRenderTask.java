package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Selectors;
import net.shipilev.fjptrace.TaskSubgraphs;
import net.shipilev.fjptrace.WorkerStatusHolder;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;

public class TaskSubgraphRenderTask extends AbstractGraphTask {
    private final TaskSubgraphs data;
    private final Map<Integer, Color> colors;

    public TaskSubgraphRenderTask(Events events, TaskSubgraphs data) {
        super(events, "Task subgraphs", "subgraphs.png");
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
