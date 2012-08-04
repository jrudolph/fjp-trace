package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.Options;
import net.shipilev.fjptrace.TaskStatus;
import net.shipilev.fjptrace.util.PairedList;
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

    private final Events events;
    private final TaskStatus taskStatus;
    private final int width;
    private final int height;
    private final String prefix;

    public TaskStatsRenderTask(Options opts, Events events, TaskStatus taskStatus) {
        this.width = opts.getWidth();
        this.height = opts.getHeight();
        this.prefix = opts.getTargetPrefix();
        this.events = events;
        this.taskStatus = taskStatus;
    }

    @Override
    protected void compute() {
        ForkJoinTask.invokeAll(
                new TaskStatsGraphTask(events, taskStatus.getSelf().filter(1), prefix + "-exectime-exclusive.png", "Task execution time (exclusive)", "Time to execute, usec"),
                new TaskStatsGraphTask(events, taskStatus.getTotal().filter(1), prefix + "-exectime-inclusive.png", "Task execution times (inclusive, including subtasks)", "Time to execute, usec")
        );
    }

    public class TaskStatsGraphTask extends LoggedRecursiveAction {

        private final Events events;
        private final PairedList data;
        private final String filename;
        private final String chartLabel;
        private final String yLabel;

        private TaskStatsGraphTask(Events events, PairedList data, String filename, String chartLabel, String yLabel) {
            super("Task statistics render \"" + chartLabel + "\"");
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

            final XYPlot plot = chart.getXYPlot();
            XYDotRenderer renderer = new XYDotRenderer();
            renderer.setDefaultEntityRadius(3);
            renderer.setSeriesPaint(0, Color.GREEN);
            plot.setRenderer(renderer);
            plot.setBackgroundPaint(Color.black);
            plot.setForegroundAlpha(0.65f);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setOutlinePaint(Color.LIGHT_GRAY);

            final ValueAxis domainAxis = plot.getDomainAxis();
            domainAxis.setTickMarkPaint(Color.white);
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            domainAxis.setInverted(true);
            domainAxis.setLowerBound(TimeUnit.NANOSECONDS.toMillis(events.getStart()));
            domainAxis.setUpperBound(TimeUnit.NANOSECONDS.toMillis(events.getEnd()));

            final ValueAxis rangeAxis = new LogarithmicAxis(yLabel);
            rangeAxis.setTickMarkPaint(Color.white);
            rangeAxis.setStandardTickUnits(new StandardTickUnitSource());
            plot.setRangeAxis(rangeAxis);

            AxisSpace space = new AxisSpace();
            space.setLeft(50);

            plot.setFixedDomainAxisSpace(space);

            /**
             * Render histogram
             */

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
            histDataSet.addSeries("H1", Arrays.copyOf(values, c), width);

            final JFreeChart histChart = ChartFactory.createHistogram(
                    chartLabel,
                    "", "Samples",
                    histDataSet,
                    PlotOrientation.VERTICAL,
                    false, false, false
            );

            histChart.setBackgroundPaint(Color.white);
            XYPlot histPlot = histChart.getXYPlot();

            rangeAxis.setAutoRange(false);
            histPlot.setDomainAxis(rangeAxis);
            histPlot.setFixedRangeAxisSpace(space);
            histPlot.setBackgroundPaint(Color.black);
            histPlot.getRenderer().setSeriesPaint(0, Color.GREEN);

            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bi.createGraphics();

            histChart.draw(graphics, new Rectangle(0, 0, width, 200));
            chart.draw(graphics, new Rectangle(0, 200, width, height - 200));

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