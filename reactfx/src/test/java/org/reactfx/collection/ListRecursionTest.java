package org.reactfx.collection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.junit.Test;

public class ListRecursionTest {

    /**
     * Tests that list changes are accumulated on recursion.
     */
    @Test
    public void test() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        ObsList<Integer> lengths = ObsList.map(strings, String::length);

        IntegerProperty firstListener = new SimpleIntegerProperty(0);

        List<Integer> first1Removed = new ArrayList<>();
        List<Integer> first1Added = new ArrayList<>();
        List<Integer> first2Removed = new ArrayList<>();
        List<Integer> first2Added = new ArrayList<>();
        List<Integer> secondRemoved = new ArrayList<>();
        List<Integer> secondAdded = new ArrayList<>();

        IntFunction<ListChangeListener<Integer>> listenerFactory = id -> ch -> {
            while(ch.next()) {
                if(firstListener.get() == 0) {
                    firstListener.set(id);
                    first1Removed.addAll(ch.getRemoved());
                    first1Added.addAll(ch.getAddedSubList());
                    strings.add(2, "55555");
                } else if(firstListener.get() == id) {
                    first2Removed.addAll(ch.getRemoved());
                    first2Added.addAll(ch.getAddedSubList());
                } else {
                    secondRemoved.addAll(ch.getRemoved());
                    secondAdded.addAll(ch.getAddedSubList());
                }
            }
        };

        lengths.addListener(listenerFactory.apply(1));
        lengths.addListener(listenerFactory.apply(2));

        strings.set(1, "4444");

        assertEquals(Arrays.asList(2), first1Removed);
        assertEquals(Arrays.asList(4), first1Added);
//        assertEquals(Arrays.asList(), first2Removed);
//        assertEquals(Arrays.asList(5), first2Added);
        assertEquals(Arrays.asList(2), secondRemoved);
        assertEquals(Arrays.asList(4, 5), secondAdded);
    }

}
