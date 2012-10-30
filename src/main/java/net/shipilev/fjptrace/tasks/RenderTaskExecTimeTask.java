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
import net.shipilev.fjptrace.TaskStatus;
import net.shipilev.fjptrace.util.PairedList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class RenderTaskExecTimeTask extends RecursiveAction {

    private final Events events;
    private final TaskStatus taskStatus;
    private final int width;
    private final int height;
    private final String prefix;
    private final long fromTime;
    private final long toTime;

    public RenderTaskExecTimeTask(Options opts, Events events, TaskStatus taskStatus) {
        this.width = opts.getWidth();
        this.height = opts.getHeight();
        this.prefix = opts.getTargetPrefix();
        this.fromTime = opts.getFromTime();
        this.toTime = opts.getToTime();
        this.events = events;
        this.taskStatus = taskStatus;
    }

    @Override
    protected void compute() {
        ForkJoinTask.invokeAll(
                new TaskStatsGraphTask(events, taskStatus.getSelf(), prefix + "-exectimeExclusive.png", "Task execution time (exclusive)", "Time to execute, sec, LOG scale"),
                new TaskStatsGraphTask(events, taskStatus.getTotal(), prefix + "-exectimeInclusive.png", "Task execution times (inclusive, including subtasks)", "Time to execute, sec, LOG scale")
        );
    }

    public class TaskStatsGraphTask extends LoggedRecursiveAction {

        private final Events events;
        private final Map<Integer, PairedList> data;
        private final String filename;
        private final String chartLabel;
        private final String yLabel;

        private TaskStatsGraphTask(Events events, Map<Integer, PairedList> data, String filename, String chartLabel, String yLabel) {
            super("Task statistics render \"" + chartLabel + "\"");
            this.events = events;
            this.data = data;
            this.filename = filename;
            this.chartLabel = chartLabel;
            this.yLabel = yLabel;
        }

        @Override
        protected void doWork() {
            DescriptiveStatistics rangeStatistics = new DescriptiveStatistics();

            final XYSeriesCollection dataset = new XYSeriesCollection();
            for (Integer depth : data.keySet()) {
                XYSeries series = new XYSeries("d(" + depth + ")");
                for (PairedList.Pair entry : data.get(depth)) {
                    double y = nanosToSeconds(entry.getK2());
                    if (y > 0) {
                        series.add(nanosToSeconds(entry.getK1()), y, false);
                        rangeStatistics.addValue(y);
                    }
                }
                dataset.addSeries(series);
            }

            final JFreeChart chart = ChartFactory.createXYLineChart(
                    "",
                    "Run time, sec", yLabel,
                    dataset,
                    PlotOrientation.HORIZONTAL,
                    true, false, false
            );

            chart.setBackgroundPaint(Color.white);

            int pointSize = (int) Math.min(10, Math.max(1, width * height / rangeStatistics.getN()));

            final XYPlot plot = chart.getXYPlot();
            XYDotRenderer renderer = new XYDotRenderer();
            renderer.setDotHeight(pointSize);
            renderer.setDotWidth(pointSize);
            enforceSeriesPaint(renderer);
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
            domainAxis.setLowerBound(nanosToSeconds(Math.max(fromTime, events.getStart())));
            domainAxis.setUpperBound(nanosToSeconds(Math.min(toTime, events.getEnd())));

            final LogAxis rangeAxis = new LogAxis(yLabel);
            rangeAxis.setTickMarkPaint(Color.white);
            rangeAxis.setStandardTickUnits(new StandardTickUnitSource());
            rangeAxis.setMinorTickCount(10);
            rangeAxis.setMinorTickMarksVisible(true);
            rangeAxis.setBase(10);
            rangeAxis.setLowerBound(rangeStatistics.getPercentile(1));
            rangeAxis.setUpperBound(rangeStatistics.getPercentile(99));

            final DecimalFormatSymbols newSymbols = new DecimalFormatSymbols(Locale.GERMAN);
            newSymbols.setExponentSeparator("E");
            final DecimalFormat decForm = new DecimalFormat("0.##E0#");
            decForm.setDecimalFormatSymbols(newSymbols);
            rangeAxis.setNumberFormatOverride(decForm);

            plot.setRangeAxis(rangeAxis);

            AxisSpace space = new AxisSpace();
            space.setLeft(50);

            plot.setFixedDomainAxisSpace(space);

            /**
             * Render Y histogram
             */
            final JFreeChart histY = getYHistogram(rangeAxis, space);

            /**
             * Render X histogram
             */
            final JFreeChart histX = getXHistogram(domainAxis, space);

            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bi.createGraphics();

            int top = 200;
            int right = 200;

            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            histX.draw(graphics, new Rectangle(width - right, top, right, height - top));
            histY.draw(graphics, new Rectangle(0, 0, width - right, top));
            chart.draw(graphics, new Rectangle(0, top, width - right, height - top));

            try {
                FileOutputStream out = new FileOutputStream(filename);
                ChartUtilities.writeBufferedImageAsPNG(out, bi);
                out.close();
            } catch (IOException e) {
                // do nothing
            }
        }

        private JFreeChart getYHistogram(ValueAxis rangeAxis, AxisSpace space) {
            final HistogramDataset histDataSet = new HistogramDataset();
            for (Integer depth : data.keySet()) {
                long[] d = data.get(depth).filter(1).getAllY();
                double[] values = new double[d.length];
                for (int c = 0; c < d.length; c++) {
                    values[c] = nanosToSeconds(d[c]);
                }
                histDataSet.addSeries("depth = " + depth, values, width);
            }

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
            enforceSeriesPaint(histPlot.getRenderer());
            return histChart;
        }

        private JFreeChart getXHistogram(ValueAxis axis, AxisSpace space) {
            final HistogramDataset histDataSet = new HistogramDataset();
            for (Integer depth : data.keySet()) {
                long[] d = data.get(depth).filter(1).getAllX();
                double[] values = new double[d.length];
                for (int c = 0; c < d.length; c++) {
                    values[c] = nanosToSeconds(d[c]);
                }
                histDataSet.addSeries("depth = " + depth, values, width);
            }

            final JFreeChart histChart = ChartFactory.createHistogram(
                    "",
                    "", "Samples",
                    histDataSet,
                    PlotOrientation.HORIZONTAL,
                    false, false, false
            );

            histChart.setBackgroundPaint(Color.white);
            XYPlot histPlot = histChart.getXYPlot();

            axis.setAutoRange(false);
            histPlot.setDomainAxis(axis);
            histPlot.setBackgroundPaint(Color.black);
            enforceSeriesPaint(histPlot.getRenderer());
            return histChart;
        }

        /**
         * Use to enforce the same color scheme.
         * @param renderer
         */
        private void enforceSeriesPaint(XYItemRenderer renderer) {
            renderer.setSeriesPaint(0, Color.WHITE);
            renderer.setSeriesPaint(1, Color.RED);
            renderer.setSeriesPaint(2, Color.GREEN);
            renderer.setSeriesPaint(3, Color.BLUE);
            renderer.setSeriesPaint(4, Color.YELLOW);
            renderer.setSeriesPaint(5, Color.MAGENTA);
            renderer.setSeriesPaint(6, Color.LIGHT_GRAY);
            Random r = new Random(1);
            for (int c = 7; c < 100; c++) {
                renderer.setSeriesPaint(c, new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
            }
        }


    }

    public static double nanosToMillis(long nanos) {
        return nanos * 1.0 / (1_000_000);
    }

    public static double nanosToSeconds(long nanos) {
        return nanos * 1.0 / (1_000_000_000);
    }

}