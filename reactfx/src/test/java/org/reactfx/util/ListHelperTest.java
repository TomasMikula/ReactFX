package org.reactfx.util;

import static org.junit.Assert.*;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.junit.Test;

public class ListHelperTest {

    @Test
    public void testAddWhileIterating() {
        ObjectProperty<ListHelper<Integer>> lh = new SimpleObjectProperty<>(null);
        IntegerProperty iterations = new SimpleIntegerProperty(0);

        lh.set(ListHelper.add(lh.get(), 0));
        lh.set(ListHelper.add(lh.get(), 1));
        lh.set(ListHelper.add(lh.get(), 2));

        ListHelper.forEach(lh.get(), i -> {
            lh.set(ListHelper.remove(lh.get(), 2-i));
            iterations.set(iterations.get() + 1);
        });

        assertEquals(3, iterations.get());
        assertEquals(0, ListHelper.size(lh.get()));
    }

    @Test
    public void testRemoveWhileIterating() {
        ObjectProperty<ListHelper<Integer>> lh = new SimpleObjectProperty<>(null);
        IntegerProperty iterations = new SimpleIntegerProperty(0);

        lh.set(ListHelper.add(lh.get(), 0));
        lh.set(ListHelper.add(lh.get(), 1));
        lh.set(ListHelper.add(lh.get(), 2));

        ListHelper.forEach(lh.get(), i -> {
            lh.set(ListHelper.add(lh.get(), 2-i));
            iterations.set(iterations.get() + 1);
        });

        assertEquals(3, iterations.get());
        assertArrayEquals(new Integer[] { 0, 1, 2, 2, 1, 0 }, ListHelper.toArray(lh.get(), n -> new Integer[n]));
    }
}
