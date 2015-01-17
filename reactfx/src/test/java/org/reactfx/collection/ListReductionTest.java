package org.reactfx.collection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.Test;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

public class ListReductionTest {

    @Test
    public void testWhenUnbound() {
        ObservableList<Integer> list = FXCollections.observableArrayList(1, 1, 1, 1, 1);
        Val<Integer> sum = LiveList.reduce(list, (a, b) -> a + b);

        assertEquals(5, sum.getValue().intValue());

        list.addAll(2, Arrays.asList(2, 2));
        assertEquals(9, sum.getValue().intValue());

        list.clear();
        assertNull(sum.getValue());
    }

    @Test
    public void testWhenBound() {
        ObservableList<Integer> list = FXCollections.observableArrayList(1, 1, 1, 1, 1);
        Val<Integer> sum = LiveList.reduce(list, (a, b) -> a + b);
        Var<Integer> lastObserved = Var.newSimpleVar(sum.getValue());

        assertEquals(5, lastObserved.getValue().intValue());

        sum.addListener((obs, oldVal, newVal) -> {
            assertEquals(lastObserved.getValue(), oldVal);
            lastObserved.setValue(newVal);
        });

        list.addAll(2, Arrays.asList(2, 2));
        assertEquals(9, lastObserved.getValue().intValue());

        list.subList(3, 6).clear();
        assertEquals(5, lastObserved.getValue().intValue());
    }

    @Test
    public void testMultipleModificationsWhenBound() {
        SuspendableList<Integer> list = new LiveArrayList<>(1, 1, 1, 1, 1).suspendable();
        Val<Integer> sum = list.reduce((a, b) -> a + b);
        Var<Integer> lastObserved = Var.newSimpleVar(null);
        sum.observeChanges((obs, oldVal, newVal) -> lastObserved.setValue(newVal));
        list.suspendWhile(() -> {
            list.addAll(0, Arrays.asList(3, 2));
            list.remove(4, 6);
            list.addAll(4, Arrays.asList(8, 15));
        });
        assertEquals(31, lastObserved.getValue().intValue());
    }

    @Test
    public void testRecursion() {
        SuspendableList<Integer> list = new LiveArrayList<>(1, 1, 1, 1, 1).suspendable();
        Val<Integer> sum = list.reduce((a, b) -> a + b);
        Var<Integer> lastObserved = Var.newSimpleVar(null);
        Random random = new Random(0xcafebabe);
        sum.addListener(obs -> {
            Integer newVal = sum.getValue();
            if(newVal < 1000) {
                list.suspendWhile(() -> {
                    // remove 4 items
                    int i = random.nextInt(list.size() - 3);
                    list.subList(i, i + 3).clear();
                    list.remove(random.nextInt(list.size()));

                    // insert 5 items
                    list.addAll(random.nextInt(list.size()), Arrays.asList(
                            random.nextInt(12), random.nextInt(12), random.nextInt(12)));
                    list.add(random.nextInt(list.size()), random.nextInt(12));
                    list.add(random.nextInt(list.size()), random.nextInt(12));
                });
            }
        });
        sum.observeChanges((obs, oldVal, newVal) -> lastObserved.setValue(newVal));
        list.set(2, 0);
        assertThat(lastObserved.getValue().intValue(), greaterThanOrEqualTo(1000));
    }
}
