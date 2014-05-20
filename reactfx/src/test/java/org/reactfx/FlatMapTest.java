package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FlatMapTest {

    @Test
    public void test() {
        EventSource<Integer> source = new EventSource<>();
        EventSource<String> a = new EventSource<>();
        EventSource<String> b = new EventSource<>();

        EventStream<String> stream = source.flatMap(i -> i == 1 ? a : b);

        List<String> emitted = new ArrayList<>();
        stream = stream.hook(s -> emitted.add(s));

        source.push(1);
        a.push("a");
        assertEquals(0, emitted.size()); // not yet subscribed

        Subscription pin = stream.pin();
        source.push(1);
        a.push("a");
        assertEquals(Arrays.asList("a"), emitted);
        emitted.clear();

        source.push(2);
        a.push("A"); // ignored
        b.push("b");
        assertEquals(Arrays.asList("b"), emitted);
        emitted.clear();

        pin.unsubscribe();
        a.push("x");
        b.push("y");
        assertEquals(0, emitted.size());

        pin = stream.pin();
        a.push("x");
        b.push("y");
        assertEquals(0, emitted.size()); // source hasn't emitted yet

        source.push(1);
        a.push("x");
        b.push("y");
        assertEquals(Arrays.asList("x"), emitted);
    }

}
