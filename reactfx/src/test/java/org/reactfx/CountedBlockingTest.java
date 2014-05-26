package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactfx.inhibeans.value.CountingListener;
import org.reactfx.inhibeans.value.ObservableValueBase;

public class CountedBlockingTest {

    @Test
    public void testIndicator() {
        Indicator a = new Indicator();
        Guard g = a.on();
        Guard h = a.on();
        g.close();
        assertTrue(a.isOn());
        g.close();
        assertTrue(a.isOn());
        h.close();
        assertFalse(a.isOn());
    }

    @Test
    public void testObservableValueBase() {
        class A extends ObservableValueBase<String> {
            private String value;

            @Override
            public String getValue() {
                return value;
            }

            public void set(String value) {
                this.value = value;
                fireValueChangedEvent();
            }
        }

        A a = new A();
        CountingListener counter = new CountingListener(a);
        Guard g = a.block();
        a.set("x");
        assertEquals(0, counter.get());
        Guard h = a.block();
        g.close();
        assertEquals(0, counter.get());
        g.close();
        assertEquals(0, counter.get());
        h.close();
        assertEquals(1, counter.get());
    }
}
