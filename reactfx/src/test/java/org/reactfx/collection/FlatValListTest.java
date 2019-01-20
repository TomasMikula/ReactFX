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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;


/**
 * @author Clément Fournier
 */
@SuppressWarnings({"unchecked"}) // bc of generic array creation in varargs
public class FlatValListTest {


    @Rule
    public ExpectedException expected = ExpectedException.none();

    private List<String> removed;
    private List<String> added;
    private List<Integer> fromIdx;
    private List<Integer> toIdx;
    private Var<Integer> calls;


    @Before
    public void testSetup() {

        removed = new ArrayList<>();
        added = new ArrayList<>();
        fromIdx = new ArrayList<>();
        toIdx = new ArrayList<>();
        calls = Var.newSimpleVar(0);
    }


    private void addListModObserver(LiveList<String> flat) {
        flat.observeChanges(ch -> {
            for (ListModification<? extends String> mod : ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
                fromIdx.add(mod.getFrom());
                toIdx.add(mod.getTo());
                calls.setValue(calls.getValue() + 1);
            }
        });
    }


    @Test
    public void testNoChange() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> strings = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(strings);

        assertEquals(asList("foo", "bar"), flat);
        assertEquals(2, flat.size());
    }


    @Test
    public void testElementChanges() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> strings = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(strings);

        flat.observeChanges(ch -> {
            for (ListModification<? extends String> mod : ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
                calls.setValue(calls.getValue() + 1);

            }
        });

        obsA.setValue("fan");

        assertEquals(singletonList("foo"), removed);
        assertEquals(singletonList("fan"), added);
        assertEquals(1, (int) calls.getValue());

        assertEquals(asList("fan", "bar"), flat);
    }


    @Test
    public void testSourceListAddition() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(source);

        addListModObserver(flat);

        source.add(Var.newSimpleVar("foobar"));

        assertEquals(emptyList(), removed);
        assertEquals(singletonList("foobar"), added);
        assertEquals(singletonList(2), fromIdx);
        assertEquals(singletonList(3), toIdx);

        assertEquals(asList("foo", "bar", "foobar"), flat);
    }


    @Test
    public void testSourceListRemoval() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(source);

        addListModObserver(flat);

        source.remove(obsB);

        assertEquals(singletonList("bar"), removed);
        assertEquals(emptyList(), added);
        assertEquals(singletonList(1), fromIdx);
        assertEquals(singletonList(1), toIdx);

        assertEquals(singletonList("foo"), flat);

    }


    @Test
    public void testSourceListReplacementDups() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(source);

        addListModObserver(flat);

        source.set(0, obsB); // it's duplicated now

        assertEquals(singletonList("foo"), removed);
        assertEquals(singletonList("bar"), added);
        assertEquals(singletonList(0), fromIdx);
        assertEquals(singletonList(1), toIdx);

        assertEquals(asList("bar", "bar"), flat);

        obsB.setValue("cou");
        assertEquals(asList("cou", "cou"), flat);

    }


    @Test
    public void testSourceListReplacement() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");
        Var<String> obsC = Var.newSimpleVar("sfam");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(source);

        addListModObserver(flat);

        assertEquals(asList("foo", "bar"), flat);

        source.set(0, obsC);

        assertEquals(singletonList("foo"), removed);
        assertEquals(singletonList("sfam"), added);
        assertEquals(singletonList(0), fromIdx);
        assertEquals(singletonList(1), toIdx);
        assertEquals(1, (int) calls.getValue());

        assertEquals(asList("sfam", "bar"), flat);

        obsB.setValue("cou");
        assertEquals(asList("sfam", "cou"), flat);
        assertEquals(2, (int) calls.getValue());

        obsA.setValue("bro");// no change
        assertEquals(asList("sfam", "cou"), flat);
        assertEquals(2, (int) calls.getValue());

        obsC.setValue("air"); // a change
        assertEquals(asList("air", "cou"), flat);
        assertEquals(3, (int) calls.getValue());

    }


    @Test
    public void testAddIsImpossible() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(source);

        expected.expect(UnsupportedOperationException.class);

        flat.add("FOO");


    }


    @Test
    public void testRemoveIsImpossible() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(source);

        expected.expect(UnsupportedOperationException.class);

        assertTrue(flat.contains("bar"));
        flat.remove("bar");
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

        LiveList<String> flat = LiveList.flattenVals(source);

        assertEquals(0, evaluations.get()); // nothing yet

        source.remove(0);
        assertEquals(0, evaluations.get());

        // less lazy that a MappedList
        // because it has to observe the changes of elements & ValBase asks for an initial valid value

        flat.observeChanges(ch -> {});
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

        LiveList<String> flat = LiveList.flattenVals(source);

        assertEquals(0, evaluations.get());

        source.remove(obsB);

        flat.observeChanges(ch -> {});

        assertEquals(0, evaluations.get()); // no more evaluation

        assertEquals(asList("foo", "kro"), flat);

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

        LiveList<String> flat = LiveList.flattenVals(source);

        assertEquals(0, evaluations.get()); // nothing yet

        SuspendableList<String> suspendable = flat.suspendable();

        // tbh I don't really know what this should be doing,
        // I adapted it from the MappedList tests
        suspendable.observeChanges(ch -> {});
        suspendable.suspendWhile(() -> {
            source.remove(1);
            source.set(1, Var.newSimpleVar("koko"));
        });

        assertEquals(3, evaluations.get());

        obsB.setValue("kirikou");

        assertEquals(asList("foo", "koko"), flat);

        assertEquals(3, evaluations.get()); // no more calls

    }


    @Test
    public void testChangeStream() {
        Var<String> obsA = Var.newSimpleVar("foo");
        Var<String> obsB = Var.newSimpleVar("bar");
        Var<String> obsC = Var.newSimpleVar("sfam");

        ObservableList<Var<String>> source = observableArrayList(obsA, obsB);
        LiveList<String> flat = LiveList.flattenVals(source);

        Var<Integer> calls = Var.newSimpleVar(0);
        Subscription sub = flat.changes().subscribe(ch -> {
            for (ListModification<? extends String> mod : ch.getModifications()) {
                calls.setValue(calls.getValue() + 1);
            }
        });

        assertEquals(asList("foo", "bar"), flat);

        source.set(0, obsC);
        assertEquals(asList("sfam", "bar"), flat);
        assertEquals(1, (int) calls.getValue());

        obsB.setValue("cou");
        assertEquals(asList("sfam", "cou"), flat);
        assertEquals(2, (int) calls.getValue());

        obsA.setValue("bro");// no change
        assertEquals(asList("sfam", "cou"), flat);
        assertEquals(2, (int) calls.getValue());


        sub.unsubscribe();

        obsB.setValue("plot");
        assertEquals(asList("sfam", "plot"), flat);
        assertEquals(2, (int) calls.getValue()); // no call


    }


}
