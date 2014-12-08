package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class DistinctStreamTest {
    <T> void testDistinct(List<T> input, List<T> expectedOutput) {
        EventSource<T> source = new EventSource<>();
        EventStream<T> distinct = new DistinctStream<>(source);
        List<T> distinctCollector = new ArrayList<>();
        distinct.subscribe(distinctCollector::add);
        input.forEach(source::push);
        assertEquals(expectedOutput, distinctCollector);
    }

    @Test
    public void testStream() {
        testDistinct(
            Arrays.asList(0, 1, 1, 0),
            Arrays.asList(0, 1, 0)
        );
    }

    @Test
    public void testStreamWithNulls() {
        testDistinct(
            Arrays.asList(null, null, 1, null, null),
            Arrays.asList(null, 1, null)
        );
    }
}
