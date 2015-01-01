package org.reactfx.util;

import static org.junit.Assert.*;

import java.util.Iterator;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.junit.Test;

public class ListHelperTest {

    @Test
    public void testRemoveWhileIterating() {
        ListHelper<Integer> lh = null;

        lh = ListHelper.add(lh, 0);
        lh = ListHelper.add(lh, 1);
        lh = ListHelper.add(lh, 2);

        Iterator<Integer> it = ListHelper.iterator(lh);
        int i = 2;
        while(it.hasNext()) {
            lh = ListHelper.remove(lh, i--);
            it.next();
        }

        assertEquals(-1, i);
        assertEquals(0, ListHelper.size(lh));
    }

    @Test
    public void testAddWhileIterating() {
        ListHelper<Integer> lh = null;

        lh = ListHelper.add(lh, 0);
        lh = ListHelper.add(lh, 1);
        lh = ListHelper.add(lh, 2);

        Iterator<Integer> it = ListHelper.iterator(lh);
        int i = 2;
        while(it.hasNext()) {
            lh = ListHelper.add(lh, i--);
            it.next();
        };

        assertEquals(-1, i);
        assertArrayEquals(new Integer[] { 0, 1, 2, 2, 1, 0 }, ListHelper.toArray(lh, n -> new Integer[n]));

        it = ListHelper.iterator(lh);
        assertFalse(lh == ListHelper.add(lh, 5)); // test that a copy is made
        while(it.hasNext()) it.next(); // drain the iterator
        assertTrue(lh == ListHelper.add(lh, 5)); // test that change is made in place
    }

    @Test
    public void testRemoveInForEach() {
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
    public void testAddInForEach() {
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
