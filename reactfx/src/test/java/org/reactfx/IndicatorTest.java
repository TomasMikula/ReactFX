package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;

public class IndicatorTest {

    @Test
    public void test() {
        Indicator indicator = new Indicator();
        Counter counter = new Counter();
        indicator.addListener(obs -> counter.inc());

        Guard g = indicator.on();

        assertEquals(1, counter.get());

        indicator.onWhile(() -> {});

        assertEquals(1, counter.get());

        g.close();

        assertEquals(2, counter.get());
    }

}
