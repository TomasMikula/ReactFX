package org.reactfx.value;

import static org.junit.Assert.*;

import java.util.function.UnaryOperator;

import org.junit.Test;
import org.reactfx.Subscription;

public class MapDynamicTest {

    @Test
    public void test() {
        Var<Integer> src = Var.newSimpleVar(1);
        Var<UnaryOperator<Integer>> fn = Var.newSimpleVar(UnaryOperator.identity());
        Val<Integer> mapped = src.mapDynamic(fn);

        assertEquals(1, mapped.getValue().intValue());

        src.setValue(2);
        assertEquals(2, mapped.getValue().intValue());

        fn.setValue(i -> i + i);
        assertEquals(4, mapped.getValue().intValue());

        Subscription sub = mapped.observeChanges((obs, oldVal, newVal) -> {
            assertEquals(4, oldVal.intValue());
            assertEquals(8, newVal.intValue());
        });
        fn.setValue(i -> i * i * i);
        sub.unsubscribe();

        fn.setValue(null);
        assertTrue(mapped.isEmpty());
    }

}
