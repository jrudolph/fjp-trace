package net.shipilev.fjptrace;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

public class TaskStatsRenderTask extends RecursiveAction {

    private static final int HEIGHT = Integer.getInteger("height", 2000);
    private static final int WIDTH = Integer.getInteger("width", 1000);
    private final Events events;
    private final TaskStatus taskStatus;

    public TaskStatsRenderTask(Events events, TaskStatus taskStatus) {
        this.events = events;
        this.taskStatus = taskStatus;
    }

    @Override
    protected void compute() {
        ForkJoinTask.invokeAll(
                new TaskStatsGraphTask(events, taskStatus.getSelf().filter(1), "exectime-exclusive.png", "Task execution time (exclusive)", "Time to execute, usec"),
                new TaskStatsGraphTask(events, taskStatus.getTotal().filter(1), "exectime-inclusive.png", "Task execution times (inclusive, including subtasks)", "Time to execute, usec")
        );
    }

    public static class TaskStatsGraphTask extends LoggedRecursiveAction {

        private final Events events;
        private final PairedList data;
        private final String filename;
        private final String chartLabel;
        private final String yLabel;

        private TaskStatsGraphTask(Events events, PairedList data, String filename, String chartLabel, String yLabel) {
            super("Task statistics render \"" + chartLabel + "\" to " + filename);
            this.events = events;
            this.data = data;
            this.filename = filename;
            this.chartLabel = chartLabel;
            this.yLabel = yLabel;
        }

        @Override
        protected void doWork() {
            XYSeries series = new XYSeries("");
            for (PairedList.Pair entry : data) {
                long x = TimeUnit.NANOSECONDS.toMillis(entry.getK1());
                long dur = TimeUnit.NANOSECONDS.toMicros(entry.getK2());
                if (dur > 0) {
                    series.add(x, dur, false);
                }
            }

            final XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(series);

            final JFreeChart chart = ChartFactory.createXYLineChart(
                    "",
                    "Run time, msec", yLabel,
                    dataset,
                    PlotOrientation.HORIZONTAL,
                    false, false, false
            );

            chart.setBackgroundPaint(Color.white);
//        chart.getLegend().setPosition(RectangleEdge.BOTTOM);

            final XYPlot plot = chart.getXYPlot();
            XYDotRenderer renderer = new XYDotRenderer();
            renderer.setDefaultEntityRadius(3);
            plot.setRenderer(renderer);
            plot.setBackgroundPaint(Color.white);
            plot.setForegroundAlpha(0.65f);
            plot.setDomainGridlinePaint(Color.gray);
            plot.setRangeGridlinePaint(Color.gray);

            final ValueAxis domainAxis = plot.getDomainAxis();
            domainAxis.setTickMarkPaint(Color.black);
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            domainAxis.setInverted(true);
            domainAxis.setLowerBound(TimeUnit.NANOSECONDS.toMillis(events.getStart()));
            domainAxis.setUpperBound(TimeUnit.NANOSECONDS.toMillis(events.getEnd()));

            final ValueAxis rangeAxis = new LogarithmicAxis(yLabel);
            rangeAxis.setTickMarkPaint(Color.black);
            rangeAxis.setStandardTickUnits(new StandardTickUnitSource());
            plot.setRangeAxis(rangeAxis);

            AxisSpace space = new AxisSpace();
            space.setLeft(50);

            plot.setFixedDomainAxisSpace(space);

            final HistogramDataset histDataSet = new HistogramDataset();
            double[] values = new double[data.getAllY().length];

            int c = 0;
            long min = Integer.MAX_VALUE;
            long max = Integer.MIN_VALUE;
            for (long l : data.getAllY()) {
                values[c++] = TimeUnit.NANOSECONDS.toMicros(l);
                min = Math.min(min, l);
                max = Math.max(max, l);
            }
            histDataSet.addSeries("H1", Arrays.copyOf(values, c), WIDTH);

            final JFreeChart histChart = ChartFactory.createHistogram(
                    chartLabel,
                    "", "Samples",
                    histDataSet,
                    PlotOrientation.VERTICAL,
                    false, false, false
            );

            histChart.setBackgroundPaint(Color.white);

            rangeAxis.setAutoRange(false);
            histChart.getXYPlot().setDomainAxis(rangeAxis);
            histChart.getXYPlot().setFixedRangeAxisSpace(space);

            BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bi.createGraphics();

            histChart.draw(graphics, new Rectangle(0, 0, WIDTH, 200));
            chart.draw(graphics, new Rectangle(0, 200, WIDTH, HEIGHT - 200));

            try {
                FileOutputStream out = new FileOutputStream(filename);
                ChartUtilities.writeBufferedImageAsPNG(out, bi);
                out.close();
            } catch (IOException e) {
                // do nothing
            }
        }

    }
}