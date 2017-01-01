package org.reactfx.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Collections;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;

import org.junit.Before;
import org.junit.Test;

public final class ObservableSortedArraySetTest {
    private ObservableSortedArraySet<IntegerProperty> set;
    private IntegerProperty prop1, prop2, prop3;

    @Before
    public void init() {
        set = new ObservableSortedArraySet<>(
            (o1, o2) -> Integer.compare(o1.get(), o2.get()),
            Collections::singleton
        );

        prop1 = new SimpleIntegerProperty(this, "prop1", 10);
        prop2 = new SimpleIntegerProperty(this, "prop2", 20);
        prop3 = new SimpleIntegerProperty(this, "prop3", 30);
    }

    @Test
    public void run() {
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

    @Test
    public void testRecursiveSetChanges() {
        int[] gotEvents = { 0 };

        set.addListener((SetChangeListener.Change<?> change) -> {
            switch (gotEvents[0]++) {
                case 0:
                    assert change.wasAdded();
                    assertThat(change.getElementAdded(), is(prop1));
                    assertThat(set, contains(prop1));
                    assertThat(set.listView(), contains(prop1));

                    set.add(prop2);
                    set.add(prop3);
                    break;

                case 1:
                    assert change.wasAdded();
                    assertThat(change.getElementAdded(), is(prop2));
                    assertThat(set, contains(prop1, prop2));
                    assertThat(set.listView(), contains(prop1, prop2));
                    break;

                case 2:
                    assert change.wasAdded();
                    assertThat(change.getElementAdded(), is(prop3));
                    assertThat(set, contains(prop1, prop2, prop3));
                    assertThat(set.listView(), contains(prop1, prop2, prop3));

                    // This won't fire another set change event, since nothing is added or removed. It will, however, still have the side effect of reordering the items.
                    prop3.set(5);
                    assertThat(set, contains(prop3, prop1, prop2));
                    assertThat(set.listView(), contains(prop3, prop1, prop2));
                    break;

                default:
                    throw new AssertionError("Spurious event: " + change);
            }
        });

        set.add(prop1);
        assertThat("wrong number of set change events", gotEvents[0], is(3));
    }

    @Test
    public void testRecursiveListViewChanges() {
        int[] gotEvents = { 0 };

        set.listView().addListener((ListChangeListener<IntegerProperty>) change -> {
            while (change.next()) {
                switch (gotEvents[0]++) {
                    case 0:
                        assert change.wasAdded();
                        assertThat(change.getFrom(), is(0));
                        assertThat(change.getTo(), is(1));
                        assertThat(change.getAddedSubList(), contains(prop1));
                        assertThat(set, contains(prop1));
                        assertThat(set.listView(), contains(prop1));

                        set.add(prop2);
                        set.add(prop3);
                        break;

                    case 1:
                        assert change.wasAdded();
                        assertThat(change.getFrom(), is(1));
                        assertThat(change.getTo(), is(2));
                        assertThat(change.getAddedSubList(), contains(prop2));
                        assertThat(set, contains(prop1, prop2));
                        assertThat(set.listView(), contains(prop1, prop2));
                        break;

                    case 2:
                        assert change.wasAdded();
                        assertThat(change.getFrom(), is(2));
                        assertThat(change.getTo(), is(3));
                        assertThat(change.getAddedSubList(), contains(prop3));
                        assertThat(set, contains(prop1, prop2, prop3));
                        assertThat(set.listView(), contains(prop1, prop2, prop3));

                        prop3.set(5);
                        break;

                    case 3:
                        assert change.wasPermutated();
                        assertThat(change.getFrom(), is(0));
                        assertThat(change.getTo(), is(3));
                        assertThat(change.getPermutation(0), is(1));
                        assertThat(change.getPermutation(1), is(2));
                        assertThat(change.getPermutation(2), is(0));
                        assertThat(set, contains(prop3, prop1, prop2));
                        assertThat(set.listView(), contains(prop3, prop1, prop2));
                        break;

                    default:
                        throw new AssertionError("Spurious event: " + change);
                }
            }
        });

        set.add(prop1);
        assertThat("wrong number of list change events", gotEvents[0], is(3));
    }
}