package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Var;

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
    public void testSuspendableVal() {
        SuspendableVar<String> a = Var.<String>newSimpleVar(null).suspendable();
        Counter counter = new Counter();
        a.addListener(obs -> counter.inc());
        Guard g = a.suspend();
        a.setValue("x");
        assertEquals(0, counter.get());
        Guard h = a.suspend();
        g.close();
        assertEquals(0, counter.get());
        g.close();
        assertEquals(0, counter.get());
        h.close();
        assertEquals(1, counter.get());
    }
}
