package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class PrependEventStreamTest {

    @Test
    public void test() {
        EventCounter countsTwice = new EventCounter();
        EventCounter countsOnce = new EventCounter();

        EventSource<Boolean> source = new EventSource<>();
        EventStream<Boolean> stream = source.prepend(true);

        stream.subscribe(countsTwice::accept);
        source.push(false);
        stream.subscribe(countsOnce::accept);

        assertEquals("Counts Twice failed", 2, countsTwice.get());
        assertEquals("Counts Once failed", 1, countsOnce.get());
    }
}
