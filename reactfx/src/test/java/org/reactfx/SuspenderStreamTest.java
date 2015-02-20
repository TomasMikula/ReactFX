package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Var;

public class SuspenderStreamTest {

    @Test
    public void test() {
        SuspendableVar<String> a = Var.newSimpleVar("foo").suspendable();
        Counter counter = new Counter();
        a.addListener((obs, oldVal, newVal) -> counter.inc());

        EventSource<Void> src = new EventSource<>();
        EventStream<Void> suspender = src.suspenderOf(a);

        suspender.hook(x -> a.setValue("bar")).subscribe(x -> {
            assertEquals(0, counter.get());
        });

        src.push(null);
        assertEquals(1, counter.get());
    }

}
