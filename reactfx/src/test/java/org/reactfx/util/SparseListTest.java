package org.reactfx.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.ForAll;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

@RunWith(Theories.class)
public class SparseListTest {

    public static class ListMod {
        final SparseList<Integer> list;
        final SparseListModification mod;

        ListMod(SparseList<Integer> list, SparseListModification mod) {
            this.list = list;
            this.mod = mod;
        }

        @Override
        public String toString() {
            return "TREE:\n" + list.getTree() + "\nMODIFICATION:\n" + mod;
        }
    }

    public static class ListMods extends Generator<ListMod> {

        public ListMods() {
            super(ListMod.class);
        }

        @Override
        public ListMod generate(
                SourceOfRandomness random, GenerationStatus status) {
            SparseList<Integer> list = SparseLists.gen(random, status);
            SparseListModification mod = SparseListModification.gen(random, list.size());
            return new ListMod(list, mod);
        }
    }

    public static class SparseLists extends Generator<SparseList<Integer>> {

        static SparseList<Integer> gen(
                SourceOfRandomness random,
                GenerationStatus status) {
            if(random.nextDouble() < 0.2) {
                return new SparseList<>();
            } else {
                SparseList<Integer> list = gen(random, status);
                SparseListModification.gen(random, list.size()).apply(list);
                return list;
            }
        }

        protected SparseLists(Class<SparseList<Integer>> type) {
            super(type);
        }

        @Override
        public SparseList<Integer> generate(
                SourceOfRandomness random,
                GenerationStatus status) {
            return gen(random, status);
        }
    }

    public static abstract class SparseListModification {

        static List<Integer> randomIntList(SourceOfRandomness random) {
            int n = random.nextInt(8);
            List<Integer> list = new ArrayList<>(n);
            for(int i = 0; i < n; ++i) {
                list.add(random.nextInt());
            }
            return list;
        }

        static SparseListModification gen(SourceOfRandomness random, int listSize) {
            int x = listSize == 0 ? random.nextInt(2, 7) : random.nextInt(8);
            switch(x) {
                case 0: return ElemUpdate  .generate(random, listSize);
                case 1: return ElemRemoval .generate(random, listSize);
                case 2: return RangeRemoval.generate(random, listSize);
                case 3: return ElemInsert  .generate(random, listSize);
                case 4: return ElemsInsert .generate(random, listSize);
                case 5: return VoidInsert  .generate(random, listSize);
                case 6: return ElemsSplice .generate(random, listSize);
                case 7: return VoidSplice  .generate(random, listSize);
                default: throw new AssertionError();
            }
        }

        abstract void apply(SparseList<Integer> list);
    }

    public static class ElemRemoval extends SparseListModification {
        static ElemRemoval generate(SourceOfRandomness random, int listSize) {
            return new ElemRemoval(random.nextInt(listSize));
        }

        private final int index;

        ElemRemoval(int index) { this.index = index; }

        @Override
        void apply(SparseList<Integer> list) {
            list.remove(index);
        }

        @Override
        public String toString() {
            return "ElemRemoval(" + index + ")";
        }
    }

    public static class RangeRemoval extends SparseListModification {
        static RangeRemoval generate(SourceOfRandomness random, int listSize) {
            int from = random.nextInt(0, listSize);
            int to = random.nextInt(from, listSize);
            return new RangeRemoval(from, to);
        }

        private final int from;
        private final int to;

        RangeRemoval(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        void apply(SparseList<Integer> list) {
            list.remove(from, to);
        }

        @Override
        public String toString() {
            return "RangeRemoval(" + from + ", " + to + ")";
        }
    }

    public static class ElemUpdate extends SparseListModification {
        static ElemUpdate generate(SourceOfRandomness random, int listSize) {
            return new ElemUpdate(
                    random.nextInt(listSize),
                    random.nextInt());
        }

        private final int index;
        private final Integer elem;

        ElemUpdate(int index, Integer elem) {
            this.index = index;
            this.elem = elem;
        }

        @Override
        void apply(SparseList<Integer> list) {
            list.set(index, elem);
        }

        @Override
        public String toString() {
            return "ElemUpdate(" + index + ", " + elem + ")";
        }
    }

    public static class ElemInsert extends SparseListModification {
        static ElemInsert generate(SourceOfRandomness random, int listSize) {
            return new ElemInsert(
                    random.nextInt(0, listSize),
                    random.nextInt());
        }

        private final int index;
        private final Integer elem;

        ElemInsert(int index, Integer elem) {
            this.index = index;
            this.elem = elem;
        }

        @Override
        void apply(SparseList<Integer> list) {
            list.insert(index, elem);
        }

        @Override
        public String toString() {
            return "ElemInsert(" + index + ", " + elem + ")";
        }
    }

    public static class ElemsInsert extends SparseListModification {
        static ElemsInsert generate(SourceOfRandomness random, int listSize) {
            return new ElemsInsert(
                    random.nextInt(0, listSize),
                    randomIntList(random));
        }

        private final int index;
        private final Collection<Integer> elems;

        ElemsInsert(int index, Collection<Integer> elems) {
            this.index = index;
            this.elems = elems;
        }

        @Override
        void apply(SparseList<Integer> list) {
            list.insertAll(index, elems);
        }

        @Override
        public String toString() {
            return "ElemsInsert(" + index + ", " + elems + ")";
        }
    }

    public static class VoidInsert extends SparseListModification {
        static VoidInsert generate(SourceOfRandomness random, int listSize) {
            return new VoidInsert(
                    random.nextInt(0, listSize),
                    random.nextInt(1024));
        }

        private final int index;
        private final int length;

        VoidInsert(int index, int length) {
            this.index = index;
            this.length = length;
        }

        @Override
        void apply(SparseList<Integer> list) {
            list.insertVoid(index, length);
        }

        @Override
        public String toString() {
            return "VoidInsert(" + index + ", " + length + ")";
        }
    }

    public static class ElemsSplice extends SparseListModification {
        static ElemsSplice generate(SourceOfRandomness random, int listSize) {
            int from = random.nextInt(0, listSize);
            int to = random.nextInt(from, listSize);
            List<Integer> ints = randomIntList(random);
            return new ElemsSplice(from, to, ints);
        }

        private final int from;
        private final int to;
        private final Collection<Integer> elems;

        ElemsSplice(int from, int to, Collection<Integer> elems) {
            this.from = from;
            this.to = to;
            this.elems = elems;
        }

        @Override
        void apply(SparseList<Integer> list) {
            list.splice(from, to, elems);
        }

        @Override
        public String toString() {
            return "ElemSplice(" + from + ", " + to + ", " + elems + ")";
        }
    }

    public static class VoidSplice extends SparseListModification {
        static VoidSplice generate(
                SourceOfRandomness random,
                int listSize) {
            int from = random.nextInt(0, listSize);
            int to = random.nextInt(from, listSize);
            return new VoidSplice(from, to, random.nextInt(1024));
        }

        private final int from;
        private final int to;
        private final int length;

        VoidSplice(int from, int to, int length) {
            this.from = from;
            this.to = to;
            this.length = length;
        }

        @Override
        void apply(SparseList<Integer> list) {
            list.spliceByVoid(from, to, length);
        }

        @Override
        public String toString() {
            return "VoidSplice(" + from + ", " + to + ", " + length + ")";
        }
    }

    private static double log2(double arg) {
        return Math.log(arg) / Math.log(2);
    }

    private static double log3(double arg) {
        return Math.log(arg) / Math.log(3);
    }

    private static int maxTreeDepth(int segments) {
        return segments == 0 ? 0 : (int) (Math.floor(log2(segments)) + 1);
    }

    private static int minTreeDepth(int segments) {
        return segments == 0 ? 0 : (int) (Math.ceil(log3(segments)) + 1);
    }

    private static int countSegments(SparseList<?> list) {
        if(list.size() == 0) {
            return 0;
        } else {
            final int n = list.size();
            int segments = 1;
            boolean lastPresent = list.isPresent(0);
            for(int i = 1; i < n; ++i) {
                if(list.isPresent(i) != lastPresent) {
                    ++segments;
                    lastPresent = !lastPresent;
                }
            }
            return segments;
        }
    }

    @Theory
    public void leafCountEqualToSegmentCount(
            @ForAll @From(ListMods.class) ListMod listMod) {
        SparseList<Integer> list = listMod.list;
        SparseListModification mod = listMod.mod;

        int segments = countSegments(list);
        int leafs = list.getTree().getLeafCount();
        assumeThat(leafs, equalTo(segments));

        mod.apply(list);

        segments = countSegments(list);
        leafs = list.getTree().getLeafCount();
        assertThat(leafs, equalTo(segments));
    }

    @Theory
    public void depthBounds(
            @ForAll @From(ListMods.class) ListMod listMod) {
        SparseList<Integer> list = listMod.list;
        SparseListModification mod = listMod.mod;

        int depth = list.getDepth();
        int n = countSegments(list);
        assumeThat(depth, lessThanOrEqualTo(maxTreeDepth(n)));
        assumeThat(depth, greaterThanOrEqualTo(minTreeDepth(n)));

        mod.apply(list);

        depth = list.getDepth();
        n = countSegments(list);
        assertThat(depth, lessThanOrEqualTo(maxTreeDepth(n)));
        assertThat(depth, greaterThanOrEqualTo(minTreeDepth(n)));
    }

    @Theory
    public void handPickedTests() {
        SparseList<Integer> list = new SparseList<>();

        list.insertVoid(0, 10);
        list.insertVoid(5, 5);
        // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
        assertThat(list.size(), equalTo(15));
        assertThat(list.getPresentCount(), equalTo(0));
        assertThat(list.getTree().getLeafCount(), equalTo(1));

        list.splice(5, 10, Collections.emptyList());
        // _ _ _ _ _ _ _ _ _ _
        assertThat(list.size(), equalTo(10));
        assertThat(list.getTree().getLeafCount(), equalTo(1));

        list.splice(5, 10, Arrays.asList(5, 6, 7, 8, 9));
        // _ _ _ _ _ 5 6 7 8 9
        assertThat(list.size(), equalTo(10));
        assertThat(list.getPresentCount(), equalTo(5));
        assertThat(list.getTree().getLeafCount(), equalTo(2));

        list.set(4, 4);
        // _ _ _ _ 4 5 6 7 8 9
        assertThat(list.size(), equalTo(10));
        assertThat(list.getPresentCount(), equalTo(6));
        assertThat(list.getTree().getLeafCount(), equalTo(2));

        list.splice(1, 2, Arrays.asList(1));
        // _ 1 _ _ 4 5 6 7 8 9
        assertThat(list.size(), equalTo(10));
        assertThat(list.getPresentCount(), equalTo(7));
        assertThat(list.getTree().getLeafCount(), equalTo(4));

        list.set(2, 2);
        // _ 1 2 _ 4 5 6 7 8 9
        assertThat(list.size(), equalTo(10));
        assertThat(list.getPresentCount(), equalTo(8));
        assertThat(list.getTree().getLeafCount(), equalTo(4));

        list.set(3, 3);
        // _ 1 2 3 4 5 6 7 8 9
        assertThat(list.size(), equalTo(10));
        assertThat(list.getPresentCount(), equalTo(9));
        assertThat(list.getTree().getLeafCount(), equalTo(2));

        assertThat(list.collect(3, 6), equalTo(Arrays.asList(3, 4, 5)));
    }
}