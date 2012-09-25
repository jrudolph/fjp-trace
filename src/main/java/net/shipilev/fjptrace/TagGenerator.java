package net.shipilev.fjptrace;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fast "unique" tag generator.
 *
 * This generator provides the thread-safe non-blocking continous stream of unique integers.
 * Due to domain constraints, this generator will wrap around every 2^31 values.
 * This would impede the uniqueness property, but the period of duplicate values is exacly 2^31.
 *
 * This generator never answers negative values.
 */
public final class TagGenerator {

    private static final int INITIAL_VALUE = 1;

    public static final int NULL_TASK_ID = -1;
    public static final int NOT_FJP_THREAD = -2;

    private final ThreadLocal<Region> regions;
    private final AtomicInteger counter;
    private final int size;

    public TagGenerator() {
        this(1_000_000);
    }

    public TagGenerator(int chunkSize) {
        if (chunkSize == 0) {
            throw new IllegalArgumentException("Chunk size should not be 0");
        }
        counter = new AtomicInteger(INITIAL_VALUE);
        regions = new ThreadLocal<>();
        size = chunkSize;
    }

    public int next() {
        Region region = regions.get();
        if (region == null || region.isEmpty()) {
            int start = counter.getAndAdd(size);
            if (start <= 0) {
                counter.set(INITIAL_VALUE);
                start = counter.getAndAdd(size);
            }
            region = new Region(start, start + size);
            regions.set(region);
        }
        return region.next();
    }

    private static final class Region {
        private final int to;
        int cur;

        public Region(int from, int to) {
            this.cur = from;
            this.to = to;
        }

        public int next() {
            return cur++;
        }

        public boolean isEmpty() {
            return cur >= to;
        }
    }

}
