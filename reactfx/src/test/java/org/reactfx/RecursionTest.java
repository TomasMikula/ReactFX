package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RecursionTest {

    @Test
    public void onRecurseQueueTest() {
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> stream = source.onRecurseQueue();
        List<Integer> emitted1 = new ArrayList<>();
        List<Integer> emitted2 = new ArrayList<>();

        stream.subscribe(x -> {
            emitted1.add(x);
            if(x > 0) source.push(x - 1);
        });
        stream.subscribe(emitted2::add);

        source.push(5);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1, 0), emitted1);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1, 0), emitted2);
    }

    @Test
    public void onRecurseRetainLatestTest() {
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> stream = source.onRecurseRetainLatest();
        List<Integer> emitted1 = new ArrayList<>();
        List<Integer> emitted2 = new ArrayList<>();

        stream.subscribe(x -> {
            emitted1.add(x);
            if(x > 0) source.push(x - 1);
        });
        stream.subscribe(emitted2::add);

        source.push(5);
        assertTrue(Arrays.asList(5, 4, 3, 2, 1, 0).equals(emitted1)
                && (Arrays.asList(0).equals(emitted2) || Arrays.asList(5, 4, 3, 2, 1, 0).equals(emitted2)));
    }
}
