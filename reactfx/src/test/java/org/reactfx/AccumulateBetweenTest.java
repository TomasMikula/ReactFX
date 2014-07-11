package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AccumulateBetweenTest {

    @Test
    public void test() {
        EventSource<Integer> source = new EventSource<>();
        EventSource<?> ticks = new EventSource<Void>();
        EventStream<Integer> queued = source.queueBetween(ticks);
        List<Integer> emitted = new ArrayList<>();
        Subscription sub = queued.subscribe(emitted::add);

        ticks.push(null);
        assertEquals(Arrays.asList(), emitted);

        source.push(1);
        source.push(2);
        assertEquals(Arrays.asList(), emitted);

        ticks.push(null);
        assertEquals(Arrays.asList(1, 2), emitted);

        ticks.push(null);
        assertEquals(Arrays.asList(1, 2), emitted);

        source.push(3);
        assertEquals(Arrays.asList(1, 2), emitted);
        sub.unsubscribe(); // should reset now
        sub = queued.subscribe(emitted::add);
        ticks.push(null);
        assertEquals(Arrays.asList(1, 2), emitted);
    }

}
