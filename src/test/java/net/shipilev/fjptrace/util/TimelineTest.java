package net.shipilev.fjptrace.util;

import junit.framework.Assert;
import org.junit.Test;

public class TimelineTest {

    @Test
    public void testBounds() {
        Timeline<Integer> t = new Timeline<Integer>();

        t.add(10, 1);
        t.add(20, 2);
        t.add(30, 3);

        Assert.assertEquals(null, t.getStatus(1));
        Assert.assertEquals(null, t.getStatus(9));
        Assert.assertEquals(Integer.valueOf(1), t.getStatus(10));
        Assert.assertEquals(Integer.valueOf(1), t.getStatus(15));
        Assert.assertEquals(Integer.valueOf(2), t.getStatus(20));
        Assert.assertEquals(Integer.valueOf(2), t.getStatus(25));
        Assert.assertEquals(Integer.valueOf(3), t.getStatus(30));
        Assert.assertEquals(null, t.getStatus(31));
        Assert.assertEquals(null, t.getStatus(40));
    }

}
