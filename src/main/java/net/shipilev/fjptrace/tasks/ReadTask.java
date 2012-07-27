package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.EventType;
import net.shipilev.fjptrace.Events;
import sun.misc.Unsafe;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

public class ReadTask extends LoggedRecursiveTask<Events> {

    private static final int OFFSET = Integer.getInteger("offset", 0);
    private static final int LIMIT = Integer.getInteger("limit", Integer.MAX_VALUE);

    private static final Unsafe U;
    private static final long BBASE;

    static {
        // steal Unsafe
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
            BBASE = U.arrayBaseOffset(byte[].class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

    }


    private final String filename;

    public ReadTask(String filename) {
        super("Reading trace file");
        this.filename = filename;
    }

    @Override
    public Events doWork() throws Exception {
        int[] relocations;
        byte[] buffer = new byte[22];

        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        int count = 0;

        /**
         * Read first time, compute relocations.
         * Relocations are needed because original file is unsorted.
         * Next step will read out entries in sorted order.
         *
         * This is tricky move to skip loading the entire file into memory.
         */
        InputStream is = new BufferedInputStream(new FileInputStream(filename));

        int size = 1;
        long[] times = new long[size];
        while (is.read(buffer) == 22) {
            long time = U.getLong(buffer, BBASE + 0);

            if (count >= size) {
                size = Math.max((int) (size * 1.2), size + 1);
                times = Arrays.copyOf(times, size);
            }

            times[count] = time;
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);

            count++;
        }
        is.close();

        reportProgress(0.25);

        times = Arrays.copyOf(times, count);

        // defensively fill
        relocations = new int[count];
        Arrays.fill(relocations, -1);

        // reduce the time domain
        for (int c = 0; c < count; c++) {
            times[c] = times[c] - minTime;
        }

        // MAGIC TRICK: mangle original index in the data
        // This will only work if we dodge the long overflow
        assert ((1.0 * (maxTime - minTime) * count) < (double) Long.MAX_VALUE) : "Magic trick failed, time to complain to developers";

        for (int c = 0; c < count; c++) {
            times[c] = times[c] * count + c;
        }

        // sorting by actual time
        Arrays.sort(times);

        // iterate in sorted order, demangle original index back
        for (int c = 0; c < count; c++) {
            int origIndex = (int) (times[c] % count);
            relocations[origIndex] = c;
        }

        // assert all relocations are in place
        for (int t : relocations) {
            assert t != -1;
        }

        /**
         * Read second time, using relocations
         */
        reportProgress(0.50);

        Events events = new Events(Math.min(LIMIT, count));
        InputStream is2 = new BufferedInputStream(new FileInputStream(filename));

        int aCount = 0;
        while (is2.read(buffer) == 22) {

            // new event index
            int index = relocations[aCount];

            long time = U.getLong(buffer, BBASE + 0);
            int eventOrd = U.getShort(buffer, BBASE + 8);
            int taskHC = U.getInt(buffer, BBASE + 10);
            long threadID = U.getLong(buffer, BBASE + 14);

            // count workers anyway
            events.addworker(threadID);

            if (0 <= index - OFFSET && index - OFFSET < LIMIT) {
                Event event = new Event(time, EventType.values()[eventOrd], threadID, taskHC);
                events.set(index, event);
            }

            aCount++;
        }
        is2.close();

        events.setBasetime(minTime);
        events.seal();

        return events;
    }
}
