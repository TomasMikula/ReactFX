package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LatestNTest {

    @Test
    public void test() {
        EventSource<Integer> src = new EventSource<>();
        EventStream<List<Integer>> latest3 = src.latestN(3);
        List<List<Integer>> emitted = new ArrayList<>();
        latest3.subscribe(emitted::add);
        src.push(1);
        src.push(2);
        src.push(3);
        src.push(4);
        src.push(5);
        assertEquals(
                Arrays.asList(
                        Arrays.asList(1),
                        Arrays.asList(1, 2),
                        Arrays.asList(1, 2, 3),
                        Arrays.asList(2, 3, 4),
                        Arrays.asList(3, 4, 5)),
                emitted);
    }

    @Test
    public void testResetOnUnsubscribe() {
        EventSource<Integer> src = new EventSource<>();
        EventStream<List<Integer>> latest3 = src.latestN(3);
        Subscription sub = latest3.pin();
        src.push(1);
        src.push(2);
        src.push(3);
        sub.unsubscribe();
        List<List<Integer>> emitted = new ArrayList<>();
        latest3.subscribe(emitted::add);
        src.push(4);
        assertEquals(Arrays.asList(4), emitted.get(0));
    }
}
