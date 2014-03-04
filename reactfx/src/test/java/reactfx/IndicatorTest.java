package reactfx;

import static org.junit.Assert.*;

import org.junit.Test;

import reactfx.Hold;
import reactfx.Indicator;
import reactfx.inhibeans.value.CountingListener;

public class IndicatorTest {

    @Test
    public void test() {
        Indicator indicator = new Indicator();
        CountingListener counter = new CountingListener(indicator);

        Hold h = indicator.on();

        assertEquals(1, counter.get());

        indicator.onWhile(() -> {});

        assertEquals(1, counter.get());

        h.close();

        assertEquals(2, counter.get());
    }

}
