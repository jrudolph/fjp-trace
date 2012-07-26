package net.shipilev.fjptrace.util;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;
import java.util.Iterator;

public class PairedList implements Iterable<PairedList.Pair> {

    private long[] k1;
    private long[] k2;

    int size;
    int index;

    public PairedList() {
        k1 = new long[1];
        k2 = new long[1];
    }

    public void add(long v1, long v2) {
        ensureCapacity(index);
        int slot = index++;
        k1[slot] = v1;
        k2[slot] = v2;
    }

    private void ensureCapacity(int newSize) {
        if (newSize >= size) {
            k1 = Arrays.copyOf(k1, multiply(newSize));
            k2 = Arrays.copyOf(k2, multiply(newSize));
            size = multiply(newSize);
        }
    }

    private int multiply(int src) {
        int dst = (int) (src * 1.2);
        return (dst > src ? dst : dst + 1);
    }

    @Override
    public Iterator<Pair> iterator() {
        return new Iterator<Pair>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < index;
            }

            @Override
            public Pair next() {
                Pair r = new Pair(k1[i], k2[i]);
                i++;
                return r;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public long[] getAllX() {
        return Arrays.copyOf(k1, index);
    }

    public long[] getAllY() {
        return Arrays.copyOf(k2, index);
    }

    public PairedList filter(double cutoff) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < index; i++) {
            stats.addValue(k2[i]);
        }

        double lower = stats.getPercentile(cutoff);
        double upper = stats.getPercentile(100 - cutoff);

        PairedList result = new PairedList();
        for (int i = 0; i < index; i++) {
            if (lower <= k2[i] && k2[i] <= upper) {
                result.add(k1[i], k2[i]);
            }
        }
        return result;
    }

    public static class Pair {
        private final long k1;
        private final long k2;

        public Pair(long k1, long k2) {
            this.k1 = k1;
            this.k2 = k2;
        }

        public long getK1() {
            return k1;
        }

        public long getK2() {
            return k2;
        }
    }
}
