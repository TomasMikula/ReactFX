package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactfx.inhibeans.value.CountingListener;

public class IndicatorTest {

    @Test
    public void test() {
        Indicator indicator = new Indicator();
        CountingListener counter = new CountingListener(indicator);

        Guard g = indicator.on();

        assertEquals(1, counter.get());

        indicator.onWhile(() -> {});

        assertEquals(1, counter.get());

        g.close();

        assertEquals(2, counter.get());
    }

}
