package org.reactfx.collection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;

import org.junit.Test;
import org.reactfx.collection.ObsList;
import org.reactfx.collection.SuspendableList;

public class SuspendableListTest {

    @Test
    public void test() {
        javafx.collections.ObservableList<Integer> base = FXCollections.observableArrayList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        SuspendableList<Integer> wrapped = ObsList.suspendable(base);
        List<Integer> mirror = new ArrayList<>(wrapped);
        wrapped.addListener((Change<? extends Integer> change) -> {
            while(change.next()) {
                if(change.wasPermutated()) {
                    List<Integer> newMirror = new ArrayList<>(mirror);
                    for(int i = 0; i < mirror.size(); ++i) {
                        newMirror.set(change.getPermutation(i), mirror.get(i));
                    }
                    mirror.clear();
                    mirror.addAll(newMirror);
                } else {
                    List<Integer> sub = mirror.subList(change.getFrom(), change.getFrom() + change.getRemovedSize());
                    sub.clear();
                    sub.addAll(wrapped.subList(change.getFrom(), change.getTo()));
                }
            }
        });

        wrapped.blockWhile(() -> {
            base.addAll(2, Arrays.asList(12, 11, 13));
            base.remove(7, 9);
            base.subList(8,  10).replaceAll(i -> i + 20);
            base.subList(4, 9).clear();
            base.addAll(4, Arrays.asList(16, 18, 25));
            base.sort(null);
            assertEquals(Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), mirror);
        });

        assertEquals(Arrays.asList(1, 9, 10, 11, 12, 16, 18, 22, 25), mirror);
    }

}
