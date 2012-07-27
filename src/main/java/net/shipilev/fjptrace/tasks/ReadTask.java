package net.shipilev.fjptrace.tasks;

import net.shipilev.fjptrace.Event;
import net.shipilev.fjptrace.EventType;
import net.shipilev.fjptrace.Events;
import net.shipilev.fjptrace.tasks.LoggedRecursiveTask;
import sun.misc.Unsafe;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

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
        Events events = new Events();
        InputStream is = new BufferedInputStream(new FileInputStream(filename));

        byte[] buffer = new byte[22];

        long basetime = Long.MAX_VALUE;
        int count = 0;
        while (is.read(buffer) == 22) {

            count++;
            long time = U.getLong(buffer, BBASE + 0);
            int eventOrd = U.getShort(buffer, BBASE + 8);
            int taskHC = U.getInt(buffer, BBASE + 10);
            long threadID = U.getLong(buffer, BBASE + 14);

            basetime = Math.min(basetime, time);

            // count workers anyway
            events.addworker(threadID);

            if (count < OFFSET) {
                continue;
            }

            if (count - LIMIT <= OFFSET) {
                Event event = new Event(time, EventType.values()[eventOrd], threadID, taskHC);
                if (!events.add(event)) {
                    throw new IllegalStateException("Duplicate event: " + event);
                }
            }
        }
        is.close();

        events.setBasetime(basetime);
        events.seal();

        return events;
    }
}
