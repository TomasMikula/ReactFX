package org.reactfx.collection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.Test;

public class ListMapTest {

    @Test
    public void testGet() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        ObsList<Integer> lengths = ObsList.map(strings, String::length);

        assertEquals(Arrays.asList(1, 2, 3), lengths);
    }

    @Test
    public void testChanges() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        ObsList<Integer> lengths = ObsList.map(strings, String::length);

        List<Integer> removed = new ArrayList<>();
        List<Integer> added = new ArrayList<>();
        lengths.observeChanges(ch -> {
            for(TransientListModification<? extends Integer> mod: ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
            }
        });

        strings.set(1, "4444");

        assertEquals(Arrays.asList(2), removed);
        assertEquals(Arrays.asList(4), added);
    }
}
