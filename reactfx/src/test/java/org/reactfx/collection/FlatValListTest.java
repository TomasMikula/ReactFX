package org.reactfx.collection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javafx.collections.FXCollections.observableArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactfx.value.Var;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;


/**
 * @author Clément Fournier
 * @since 1.0
 */
@SuppressWarnings({"Duplicates", "unchecked"})

public class FlatValListTest {


    @Rule
    public ExpectedException expected = ExpectedException.none();


    @Test
    public void testNoChange() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> strings = observableArrayList(obsA, obsB);
        LiveList<String> rx = LiveList.flattenVals(strings);

        assertEquals(asList("foo", "bar"), rx);
        assertEquals(2, rx.size());
    }


    @Test
    public void testElementChanges() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> strings = observableArrayList(obsA, obsB);
        LiveList<String> rx = LiveList.flattenVals(strings);

        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        rx.observeChanges(ch -> {
            for (ListModification<? extends String> mod : ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
            }
        });

        obsA.setValue("fan");

        assertEquals(singletonList("foo"), removed);
        assertEquals(singletonList("fan"), added);
    }


    @Test
    public void testSourceListAddition() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> rx = LiveList.flattenVals(source);

        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<Integer> fromIdx = new ArrayList<>();
        List<Integer> toIdx = new ArrayList<>();
        rx.observeChanges(ch -> {
            for (ListModification<? extends String> mod : ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
                fromIdx.add(mod.getFrom());
                toIdx.add(mod.getTo());
            }
        });

        source.add(Var.newSimpleVar("foobar"));

        assertEquals(emptyList(), removed);
        assertEquals(singletonList("foobar"), added);
        assertEquals(singletonList(2), fromIdx);
        assertEquals(singletonList(3), toIdx);

        assertEquals(asList("foo", "bar", "foobar"), rx);
    }


    @Test
    public void testSourceListRemoval() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> rx = LiveList.flattenVals(source);

        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<Integer> fromIdx = new ArrayList<>();
        List<Integer> toIdx = new ArrayList<>();
        rx.observeChanges(ch -> {
            for (ListModification<? extends String> mod : ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
                fromIdx.add(mod.getFrom());
                toIdx.add(mod.getTo());
            }
        });

        source.remove(obsB);

        assertEquals(singletonList("bar"), removed);
        assertEquals(emptyList(), added);
        assertEquals(singletonList(1), fromIdx);
        assertEquals(singletonList(1), toIdx);

        assertEquals(singletonList("foo"), rx);

    }


    @Test
    public void testSourceListReplacement() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> rx = LiveList.flattenVals(source);

        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<Integer> fromIdx = new ArrayList<>();
        List<Integer> toIdx = new ArrayList<>();
        rx.observeChanges(ch -> {
            for (ListModification<? extends String> mod : ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
                fromIdx.add(mod.getFrom());
                toIdx.add(mod.getTo());
            }
        });

        source.set(0, obsB); // it's duplicated now

        assertEquals(singletonList("foo"), removed);
        assertEquals(singletonList("bar"), added);
        assertEquals(singletonList(0), fromIdx);
        assertEquals(singletonList(1), toIdx);

        assertEquals(asList("bar", "bar"), rx);

        obsB.setValue("cou");
        assertEquals(asList("cou", "cou"), rx);

    }


    @Test
    public void testOneCannotAddToReactiveList() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> rx = LiveList.flattenVals(source);

        expected.expect(UnsupportedOperationException.class);

        rx.add("FOO");


    }


    @Test
    public void testOneCannotRemoveFromReactiveList() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> rx = LiveList.flattenVals(source);

        expected.expect(UnsupportedOperationException.class);

        assertTrue(rx.contains("bar"));
        rx.remove("bar");
    }


    @Test
    public void testLaziness() {

        IntegerProperty evaluations = new SimpleIntegerProperty(0);

        Function<String, String> sideEffectConversion = src -> {
            evaluations.set(evaluations.get() + 1);
            return src;
        };

        Var<String> obsA = Var.newSimpleVar("foo").mapBidirectional(sideEffectConversion, s -> s);
        Var<String> obsB = Var.newSimpleVar("bar").mapBidirectional(sideEffectConversion, s -> s);

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);

        LiveList<String> rx = LiveList.flattenVals(source);

        assertEquals(0, evaluations.get()); // nothing yet

        source.remove(0);
        assertEquals(0, evaluations.get());

        // less lazy that a MappedList
        // because it has to observe the changes of elements & ValBase asks for an initial valid value

        rx.observeChanges(ch -> {});
        assertEquals(1, evaluations.get());

    }


    @Test
    public void testRemovedEltIsReleased() {
        IntegerProperty evaluations = new SimpleIntegerProperty(0);

        Function<String, String> sideEffectConversion = src -> {
            evaluations.set(evaluations.get() + 1);
            return src;
        };

        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar").mapBidirectional(sideEffectConversion, s -> s);
        Var<String> obsC = Var.newSimpleVar("kro");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB, obsC);

        LiveList<String> rx = LiveList.flattenVals(source);

        assertEquals(0, evaluations.get());

        source.remove(obsB);

        rx.observeChanges(ch -> {});

        assertEquals(0, evaluations.get()); // no more evaluation

        assertEquals(asList("foo", "kro"), rx);

        assertEquals(0, evaluations.get());

    }


    @Test
    public void testLazinessOnChangeAccumulation() {
        IntegerProperty evaluations = new SimpleIntegerProperty(0);

        Function<String, String> sideEffectConversion = src -> {
            evaluations.set(evaluations.get() + 1);
            return src;
        };

        Var<String> obsA = Var.newSimpleVar("foo").mapBidirectional(sideEffectConversion, s -> s);
        Var<String> obsB = Var.newSimpleVar("bar").mapBidirectional(sideEffectConversion, s -> s);
        Var<String> obsC = Var.newSimpleVar("kro").mapBidirectional(sideEffectConversion, s -> s);

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB, obsC);

        LiveList<String> rx = LiveList.flattenVals(source);

        assertEquals(0, evaluations.get()); // nothing yet

        SuspendableList<String> suspendable = rx.suspendable();

        suspendable.observeChanges(ch -> {});
        suspendable.suspendWhile(() -> {
            source.remove(1);
            source.set(1, Var.newSimpleVar("koko"));
        });

        assertEquals(3, evaluations.get());

        obsB.setValue("kirikou");

        assertEquals(asList("foo", "koko"), rx);

        assertEquals(3, evaluations.get());

    }


}
