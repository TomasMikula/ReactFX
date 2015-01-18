package org.reactfx.value;

import static org.junit.Assert.*;
import static org.reactfx.util.Tuples.*;

import org.junit.Test;
import org.reactfx.Counter;
import org.reactfx.util.Tuple2;

public class ListenerInvocationTest {

    @Test
    public void test() {
        Counter invalidations = new Counter();
        Var<Tuple2<Integer, Integer>> observedChange = Var.newSimpleVar(null);

        Var<Integer> src = Var.newSimpleVar(1);
        Val<Integer> squared = src.map(i -> i*i);
        squared.addListener(obs -> invalidations.inc());

        assertEquals(0, invalidations.get());

        src.setValue(2);
        assertEquals(1, invalidations.getAndReset());

        src.setValue(3);
        assertEquals(0, invalidations.getAndReset());

        squared.addListener((obs, oldVal, newVal) -> {
            observedChange.setValue(t(oldVal, newVal));
        });

        assertNull(observedChange.getValue());

        src.setValue(4);
        assertEquals(1, invalidations.getAndReset());
        assertEquals(t(9, 16), observedChange.getValue());
        observedChange.setValue(null);

        src.setValue(-4);
        assertEquals(1, invalidations.getAndReset());
        assertNull(observedChange.getValue());
    }
}
