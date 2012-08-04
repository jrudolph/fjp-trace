package net.shipilev.fjptrace;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;

public class Options {
    private String source;
    private int limit;
    private int offset;
    private final String[] args;
    private int height;
    private int width;
    private String targetPrefix;

    public Options(String[] args) {
        this.args = args;
    }

    public boolean parse() throws IOException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new OptFormatter());

        OptionSpec<String> source = parser.accepts("s", "Source trace file")
                .withRequiredArg().ofType(String.class).describedAs("file").required();

        OptionSpec<String> target = parser.accepts("t", "Target file prefix")
                .withRequiredArg().ofType(String.class).describedAs("filename");

        OptionSpec<Integer> limit = parser.accepts("limit", "Limit read to N events")
                .withRequiredArg().ofType(int.class).describedAs("N").defaultsTo(Integer.MAX_VALUE);

        OptionSpec<Integer> offset = parser.accepts("offset", "Skip N events in trace file")
                .withRequiredArg().ofType(int.class).describedAs("N").defaultsTo(0);

        OptionSpec<Integer> height = parser.accepts("height", "Image height")
                .withRequiredArg().ofType(int.class).describedAs("px").defaultsTo(2000);

        OptionSpec<Integer> width = parser.accepts("width", "Image width")
                .withRequiredArg().ofType(int.class).describedAs("px").defaultsTo(1000);

        parser.accepts("h", "Print this help");

        OptionSet set;
        try {
            set = parser.parse(args);
        } catch (OptionException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        if (set.has("h")) {
            parser.printHelpOn(System.out);
            return false;
        }

        this.source = set.valueOf(source);
        this.limit = set.valueOf(limit);
        this.offset = set.valueOf(offset);
        this.height = set.valueOf(height);
        this.width = set.valueOf(width);

        if (!set.has(target)) {
            this.targetPrefix = set.valueOf(source);
        } else {
            this.targetPrefix = set.valueOf(target);
        }

        return true;
    }

    public String getSource() {
        return source;
    }

    public String getTargetPrefix() {
        return targetPrefix;
    }

    public int getSourceOffset() {
        return offset;
    }

    public int getSourceLimit() {
        return limit;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

}
