package org.reactfx.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ListsTest {

    @Test
    public void testConcatStackSafety() {
        List<String> singleton = Collections.singletonList("");
        List<String> list = singleton;

        for(int i = 0; i < 100_000; ++i) {
            list = Lists.concat(list, singleton);
        }

        list.size();
        list.get(50_000);
    }

    @Test
    public void testConcatSublistStackSafety() {
        List<String> singleton = Collections.singletonList("");
        List<String> list = new ArrayList<>(100_000);
        for(int i = 0; i < 50_000; ++i) {
            list.add("");
        }

        for(int i = 0; i < 50_000; ++i) {
            list = Lists.concat(list.subList(1, 50_000), singleton);
        }

        list.size();
        list.get(0);
    }
}
