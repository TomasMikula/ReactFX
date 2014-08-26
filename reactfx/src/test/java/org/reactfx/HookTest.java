package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;

public class HookTest {

    /**
     * Tests that the side effect is not allowed to cause recursive event
     * emission.
     */
    @Test
    public void recursionPreventionTest() {
        EventCounter eventCounter = new EventCounter();
        EventCounter errorCounter = new EventCounter();
        EventSource<Integer> source = new EventSource<>();
        source.hook(i -> source.push(i-1)).subscribe(eventCounter, errorCounter);
        source.push(5);
        assertEquals(0, eventCounter.get());
        assertEquals(1, errorCounter.get());
    }

}
