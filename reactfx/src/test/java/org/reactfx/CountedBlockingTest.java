package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Var;

public class CountedBlockingTest {

    @Test
    public void testIndicator() {
        SuspendableNo a = new SuspendableNo();
        Guard g = a.suspend();
        Guard h = a.suspend();
        g.close();
        assertTrue(a.get());
        g.close();
        assertTrue(a.get());
        h.close();
        assertFalse(a.get());
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
