package org.reactfx.value;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactfx.Counter;
import org.reactfx.Guard;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Var;

public class SuspendableVarTest {

    @Test
    public void test() {
        SuspendableVar<Boolean> property = Var.newSimpleVar(false).suspendable();
        Counter changes = new Counter();
        Counter invalidations = new Counter();
        property.addListener((obs, old, newVal) -> changes.inc());
        property.addListener(obs -> invalidations.inc());

        property.setValue(true);
        property.setValue(false);
        assertEquals(2, changes.getAndReset());
        assertEquals(2, invalidations.getAndReset());

        Guard g = property.suspend();
        property.setValue(true);
        property.setValue(false);
        g.close();
        assertEquals(0, changes.get());
        assertEquals(1, invalidations.get());
    }
}
