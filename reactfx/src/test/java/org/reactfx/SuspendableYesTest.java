package org.reactfx;

import static org.junit.Assert.*;
import static org.reactfx.util.Tuples.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.reactfx.util.Tuple2;

public class SuspendableYesTest {

    @Test
    public void test() {
        SuspendableYes sy = new SuspendableYes();

        Counter counter = new Counter();
        sy.addListener((obs, oldVal, newVal) -> {
            assertNotEquals(oldVal, newVal);
            counter.inc();
        });

        Guard g = sy.suspend();

        assertEquals(1, counter.getAndReset());

        sy.suspendWhile(() -> {});

        assertEquals(0, counter.getAndReset());

        g.close();

        assertEquals(1, counter.getAndReset());
    }

    @Test
    public void recursionTest() {
        SuspendableYes sy = new SuspendableYes();

        // first listener immediately suspends after resumed
        sy.addListener((ind, oldVal, newVal) -> {
            if(!newVal) {
                sy.suspend();
            }
        });

        // record changes observed by the second listener
        List<Tuple2<Boolean, Boolean>> changes = new ArrayList<>();
        EventStreams.changesOf(sy)
                .subscribe(ch -> changes.add(t(ch.getOldValue(), ch.getNewValue())));

        sy.suspend().close();

        for(Tuple2<Boolean, Boolean> ch: changes) {
            assertNotEquals(ch._1, ch._2);
        }

        for(int i = 0; i < changes.size() - 1; ++i) {
            assertEquals(
                    "changes[" + i + "] = " + changes.get(i) + " and changes[" + (i+1) + "] = " + changes.get(i+1) + " are not compatible",
                    changes.get(i)._2, changes.get(i+1)._1);
        }
    }
}
