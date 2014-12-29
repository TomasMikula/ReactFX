package org.reactfx;

import org.junit.Test;

public class HookTest {

    /**
     * Tests that the side effect is not allowed to cause recursive event
     * emission.
     */
    @Test(expected=IllegalStateException.class)
    public void recursionPreventionTest() {
        EventSource<Integer> source = new EventSource<>();
        source.hook(i -> source.push(i-1)).pin();
        source.push(5);
    }

}
