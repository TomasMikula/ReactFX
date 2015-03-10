package org.reactfx.collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class LiveConcatenationTest {

    private LiveArrayList<String> left;
    private LiveArrayList<String> right;
    private LiveConcatenation<String> concat;

    @Before
    public void setUp() {
        left = new LiveArrayList<>("1", "2");
        right = new LiveArrayList<>("one", "two");
        concat = new LiveConcatenation<>(left, right);
    }

    @Test
    public void testValues() {
        Assert.assertEquals(4, concat.size());
        Assert.assertEquals(Arrays.asList("1", "2", "one", "two"), concat);
    }

    @Test
    public void testAddLeft() {
        concat.observeChanges(change -> {
            Assert.assertEquals(1, change.getModificationCount());
            ListModification<? extends String> mod = change.getModifications().get(0);
            Assert.assertEquals(Collections.singletonList("3"), mod.getAddedSubList());
            Assert.assertTrue(mod.getRemoved().isEmpty());
            Assert.assertEquals(2, mod.getFrom());
        });

        left.add("3");
        Assert.assertEquals(
            Arrays.asList("1", "2", "3", "one", "two"),
            concat
        );
    }

    @Test
    public void testRemoveLeft() {
        concat.observeChanges(change -> {
            Assert.assertEquals(1, change.getModificationCount());
            ListModification<? extends String> mod = change.getModifications().get(0);
            Assert.assertEquals(Collections.singletonList("1"), mod.getRemoved());
            Assert.assertTrue(mod.getAddedSubList().isEmpty());
            Assert.assertEquals(0, mod.getFrom());
        });

        left.remove("1");
        Assert.assertEquals(
            Arrays.asList("2", "one", "two"),
            concat
        );
    }

    @Test
    public void testAddRight() {
        concat.observeChanges(change -> {
            Assert.assertEquals(1, change.getModificationCount());
            ListModification<? extends String> mod = change.getModifications().get(0);
            Assert.assertEquals(Collections.singletonList("zero"), mod.getAddedSubList());
            Assert.assertTrue(mod.getRemoved().isEmpty());
            Assert.assertEquals(2, mod.getFrom());
        });

        right.add(0, "zero");
        Assert.assertEquals(
            Arrays.asList("1", "2", "zero", "one", "two"),
            concat
        );
    }

    @Test
    public void testRemoveRight() {
        concat.observeChanges(change -> {
            Assert.assertEquals(1, change.getModificationCount());
            ListModification<? extends String> mod = change.getModifications().get(0);
            Assert.assertEquals(Collections.singletonList("two"), mod.getRemoved());
            Assert.assertTrue(mod.getAddedSubList().isEmpty());
            Assert.assertEquals(3, mod.getFrom());
        });

        right.remove(1);
        Assert.assertEquals(
            Arrays.asList("1", "2", "one"),
            concat
        );
    }

    @Test
    public void testMulti() {
        Assert.assertEquals(
            Arrays.asList("0", "1", "2", "3", "4", "5"),
            LiveConcatenation.multi(
                new LiveArrayList<>("0"),
                new LiveArrayList<>("1", "2"),
                new LiveArrayList<>("3"),
                new LiveArrayList<>("4"),
                new LiveArrayList<>("5")
            )
        );
    }
}
