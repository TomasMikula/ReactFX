package inhibeans.value;

import static org.junit.Assert.*;
import inhibeans.Hold;

import org.junit.Test;

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
