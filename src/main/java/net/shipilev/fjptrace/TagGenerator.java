package net.shipilev.fjptrace;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fast "unique" tag generator.
 *
 * This generator provides the thread-safe non-blocking continous stream of unique integers.
 * Due to domain constraints, this generator will wrap around every 2^32 values.
 * This would impede the uniqueness property, but the period of duplicate values is exacly 2^32.
 */
public final class TagGenerator {

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
        counter = new AtomicInteger(Integer.MIN_VALUE);
        regions = new ThreadLocal<>();
        size = chunkSize;
    }

    public int next() {
        Region region = regions.get();
        if (region == null || region.isEmpty()) {
            int start = counter.getAndAdd(size);
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
