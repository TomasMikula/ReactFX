package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ForgetfulEventStreamTest {

    @Test
    public void test() {
        EventSource<Integer> source = new EventSource<>();
        SuspendableEventStream<Integer> suspendable = source.forgetful();
        List<Integer> emitted = new ArrayList<>();
        suspendable.subscribe(emitted::add);

        source.push(1);
        suspendable.suspendWhile(() -> {
            source.push(2);
            source.push(3);
        });
        source.push(4);

        assertEquals(Arrays.asList(1, 3, 4), emitted);
    }

    @Test
    public void testResetOnUnsubscribe() {
        EventSource<Integer> source = new EventSource<>();
        SuspendableEventStream<Integer> suspendable = source.forgetful();
        List<Integer> emitted = new ArrayList<>();
        Subscription sub = suspendable.subscribe(emitted::add);

        Guard suspension = suspendable.suspend();
        source.push(1);
        source.push(2);
        assertEquals(Arrays.asList(), emitted);

        sub.unsubscribe(); // suspendable's stored value should have be reset now
        sub = suspendable.subscribe(emitted::add);
        suspension.close();
        assertEquals(Arrays.asList(), emitted);
    }
}
