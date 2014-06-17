package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.Test;

public class SizeOfTest {

    @Test
    public void test() {
        ObservableList<Integer> list = FXCollections.observableArrayList();
        EventStream<Integer> size = EventStreams.sizeOf(list);
        List<Integer> sizes = new ArrayList<>();
        Subscription sub = size.subscribe(sizes::add);
        list.add(1);
        list.addAll(2, 3, 4);
        assertEquals(Arrays.asList(0, 1, 4), sizes);

        sub.unsubscribe();
        sizes.clear();
        list.addAll(5, 6);
        assertEquals(Arrays.asList(), sizes);

        size.subscribe(sizes::add);
        list.addAll(7, 8);
        assertEquals(Arrays.asList(6, 8), sizes);
    }

}
