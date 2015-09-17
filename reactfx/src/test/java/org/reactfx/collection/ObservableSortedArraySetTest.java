package org.reactfx.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Collections;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.SetChangeListener;

import org.junit.Test;

public final class ObservableSortedArraySetTest {
    @Test
    public void run() {
        ObservableSortedArraySet<IntegerProperty> set = new ObservableSortedArraySet<>(
            (o1, o2) -> Integer.compare(o1.get(), o2.get()),
            Collections::singleton
        );

        IntegerProperty prop1 = new SimpleIntegerProperty(this, "prop1", 10),
            prop2 = new SimpleIntegerProperty(this, "prop2", 20),
            prop3 = new SimpleIntegerProperty(this, "prop3", 30);

        SetChangeListener.Change<?>[] lastChange = new SetChangeListener.Change<?>[1];

        set.addListener((SetChangeListener.Change<?> change) -> {
            assert lastChange[0] == null
                : "Spurious event: " + change;

            lastChange[0] = change;
        });

        assertThat(set, empty());
        assertThat(set.listView(), empty());
        assert !set.contains(prop1);
        assert !set.remove(prop1);

        assert set.add(prop2);
        assert !set.add(prop2);
        assertThat(set, contains(prop2));
        assertThat(set.listView(), contains(prop2));
        assert set.contains(prop2);
        assert !set.contains(prop1);
        assert lastChange[0].wasAdded();
        assert !lastChange[0].wasRemoved();
        assertThat(lastChange[0].getElementAdded(), is(prop2));
        lastChange[0] = null;

        assert set.add(prop1);
        assert !set.add(prop1);
        assertThat(set, contains(prop1, prop2));
        assertThat(set.listView(), contains(prop1, prop2));
        assertThat(set.listView().get(1), is(prop2));
        assertThat(set.last(), is(prop2));
        assert set.contains(prop2);
        assert set.contains(prop1);
        assert lastChange[0].wasAdded();
        assert !lastChange[0].wasRemoved();
        assertThat(lastChange[0].getElementAdded(), is(prop1));
        lastChange[0] = null;

        prop2.set(5);
        assertThat(set, contains(prop2, prop1));
        assertThat(set.listView(), contains(prop2, prop1));
        assertThat(set.listView().get(1), is(prop1));
        assertThat(set.last(), is(prop1));

        assert set.add(prop3);
        assert !set.add(prop3);
        assertThat(set, contains(prop2, prop1, prop3));
        assertThat(set.listView(), contains(prop2, prop1, prop3));
        assertThat(set.listView().get(2), is(prop3));
        assertThat(set.listView().size(), is(3));
        assertThat(set.last(), is(prop3));
        assert lastChange[0].wasAdded();
        assert !lastChange[0].wasRemoved();
        assertThat(lastChange[0].getElementAdded(), is(prop3));
        lastChange[0] = null;

        assert set.remove(prop2);
        assertThat(set, contains(prop1, prop3));
        assertThat(set.listView(), contains(prop1, prop3));
        assertThat(set.listView().get(0), is(prop1));
        assertThat(set.last(), is(prop3));
        assertThat(set.first(), is(prop1));
        assert !set.remove(prop2);
        assert !lastChange[0].wasAdded();
        assert lastChange[0].wasRemoved();
        assertThat(lastChange[0].getElementRemoved(), is(prop2));
    }
}