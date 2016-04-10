package org.reactfx.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javafx.scene.control.IndexRange;

public final class SparseList<E> {

    private static interface Segment<E> {
        boolean isPresent();
        int getLength();
        int getPresentCount();
        int getPresentCountBetween(int from, int to);
        boolean isPresent(int index);
        Optional<E> get(int index);
        E getOrThrow(int index);
        void setOrThrow(int index, E elem);
        List<E> appendTo(List<E> acc);
        List<E> appendRangeTo(List<E> acc, int from, int to);
        Segment<E> subSegment(int from, int to);
        boolean possiblyDestructiveAppend(Segment<E> suffix);

        default Stats getStatsBetween(int from, int to) {
            return new Stats(to - from, getPresentCountBetween(from, to));
        }
    }

    private static final class AbsentSegment<E> implements Segment<E> {
        private int length;

        AbsentSegment(int length) {
            assert length > 0;
            this.length = length;
        }

        @Override
        public String toString() {
            return "[Void x " + length + "]";
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getPresentCount() {
            return 0;
        }

        @Override
        public int getPresentCountBetween(int from, int to) {
            return 0;
        }

        @Override
        public boolean isPresent(int index) {
            return false;
        }

        @Override
        public Optional<E> get(int index) {
            return Optional.empty();
        }

        @Override
        public E getOrThrow(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public void setOrThrow(int index, E elem) {
            throw new NoSuchElementException();
        }

        @Override
        public List<E> appendTo(List<E> acc) {
            return acc;
        }

        @Override
        public List<E> appendRangeTo(List<E> acc, int from, int to) {
            return acc;
        }

        @Override
        public Segment<E> subSegment(int from, int to) {
            assert Lists.isValidRange(from, to, length);
            return new AbsentSegment<>(to - from);
        }

        @Override
        public boolean possiblyDestructiveAppend(Segment<E> suffix) {
            if(suffix.getPresentCount() == 0) {
                length += suffix.getLength();
                return true;
            } else {
                return false;
            }
        }
    }

    private static final class PresentSegment<E> implements Segment<E> {
        private final List<E> list;

        public PresentSegment(Collection<? extends E> c) {
            assert c.size() > 0;
            list = new ArrayList<>(c);
        }

        @Override
        public String toString() {
            return "[" + list.size() + " items: " + list + "]";
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public int getLength() {
            return list.size();
        }

        @Override
        public int getPresentCount() {
            return list.size();
        }

        @Override
        public int getPresentCountBetween(int from, int to) {
            assert Lists.isValidRange(from, to, getLength());
            return to - from;
        }

        @Override
        public boolean isPresent(int index) {
            assert Lists.isValidIndex(index, getLength());
            return true;
        }

        @Override
        public Optional<E> get(int index) {
            return Optional.of(list.get(index));
        }

        @Override
        public E getOrThrow(int index) {
            return list.get(index);
        }

        @Override
        public void setOrThrow(int index, E elem) {
            list.set(index, elem);
        }

        @Override
        public List<E> appendTo(List<E> acc) {
            acc.addAll(list);
            return acc;
        }

        @Override
        public List<E> appendRangeTo(List<E> acc, int from, int to) {
            acc.addAll(list.subList(from, to));
            return acc;
        }

        @Override
        public Segment<E> subSegment(int from, int to) {
            return new PresentSegment<>(list.subList(from, to));
        }

        @Override
        public boolean possiblyDestructiveAppend(Segment<E> suffix) {
            if(suffix.getPresentCount() == suffix.getLength()) {
                suffix.appendTo(list);
                return true;
            } else {
                return false;
            }
        }
    }

    private static final class Stats {
        private static final Stats ZERO = new Stats(0, 0);

        final int size;
        final int presentCount;

        Stats(int size, int presentCount) {
            assert size >= presentCount && presentCount >= 0;
            this.size = size;
            this.presentCount = presentCount;
        }

        int getSize() { return size; }
        int getPresentCount() { return presentCount; }
    }

    private static final MapToMonoid<Segment<?>, Stats> SEGMENT_STATS =
            new MapToMonoid<Segment<?>, Stats>() {

        @Override
        public Stats unit() {
            return Stats.ZERO;
        }

        @Override
        public Stats reduce(Stats left, Stats right) {
            return new Stats(
                    left.size + right.size,
                    left.presentCount + right.presentCount);
        }

        @Override
        public Stats apply(Segment<?> seg) {
            return new Stats(seg.getLength(), seg.getPresentCount());
        }
    };

    private static <E> FingerTree<Segment<E>, Stats> emptyTree() {
        return FingerTree.empty(SEGMENT_STATS);
    }

    private FingerTree<Segment<E>, Stats> tree;

    public SparseList() {
        tree = emptyTree();
    }

    public int size() {
        return tree.getStats().size;
    }

    public int getPresentCount() {
        return tree.getStats().presentCount;
    }

    public boolean isPresent(int index) {
        return tree.get(Stats::getSize, index, Segment::isPresent);
    }

    public E getOrThrow(int index) {
        return tree.get(Stats::getSize, index, Segment::getOrThrow);
    }

    public Optional<E> get(int index) {
        return tree.get(Stats::getSize, index, Segment::get);
    }

    public E getPresent(int presentIndex) {
        return tree.get(
                Stats::getPresentCount,
                presentIndex,
                Segment::getOrThrow);
    }

    public int getPresentCountBefore(int position) {
        Lists.checkPosition(position, size());
        return tree.getStatsBetween(
                Stats::getSize,
                0, position,
                Segment::getStatsBetween).getPresentCount();
    }

    public int getPresentCountAfter(int position) {
        return getPresentCount() - getPresentCountBefore(position);
    }

    public int getPresentCountBetween(int from, int to) {
        Lists.checkRange(from, to, size());
        return getPresentCountBefore(to) - getPresentCountBefore(from);
    }

    public int indexOfPresentItem(int presentIndex) {
        Lists.checkIndex(presentIndex, getPresentCount());
        return tree.locateProgressively(Stats::getPresentCount, presentIndex)
                .map(this::locationToPosition);
    }

    public IndexRange getPresentItemsRange() {
        if(getPresentCount() == 0) {
            return new IndexRange(0, 0);
        } else {
            int lowerBound = tree.locateProgressively(Stats::getPresentCount, 0)
                    .map(this::locationToPosition);
            int upperBound = tree.locateRegressively(Stats::getPresentCount, getPresentCount())
                    .map(this::locationToPosition);
            return new IndexRange(lowerBound, upperBound);
        }
    }

    private int locationToPosition(int major, int minor) {
        return tree.getStatsBetween(0, major).size + minor;
    }

    public List<E> collect() {
        List<E> acc = new ArrayList<E>(getPresentCount());
        return tree.fold(acc, (l, seg) -> seg.appendTo(l));
    }

    public List<E> collect(int from, int to) {
        List<E> acc = new ArrayList<E>(getPresentCountBetween(from, to));
        return tree.foldBetween(
                acc,
                (l, seg) -> seg.appendTo(l),
                Stats::getSize,
                from,
                to,
                (l, seg, start, end) -> seg.appendRangeTo(l, start, end));
    }

    public void clear() {
        tree = emptyTree();
    }

    public void remove(int index) {
        remove(index, index + 1);
    }

    public void remove(int from, int to) {
        Lists.checkRange(from, to, size());
        if(from != to) {
            spliceSegments(from, to, Collections.emptyList());
        }
    }

    public void set(int index, E elem) {
        tree.get(Stats::getSize, index).exec((seg, loc) -> {
            if(seg.isPresent()) {
                seg.setOrThrow(loc.minor, elem);
                // changing an element does not affect stats, so we're done
            } else {
                splice(index, index + 1, Collections.singleton(elem));
            }
        });
    }

    public boolean setIfAbsent(int index, E elem) {
        if(isPresent(index)) {
            return false;
        } else {
            set(index, elem);
            return true;
        }
    }

    public void insert(int position, E elem) {
        insertAll(position, Collections.singleton(elem));
    }

    public void insertAll(int position, Collection<? extends E> elems) {
        if(elems.isEmpty()) {
            return;
        }

        PresentSegment<E> seg = new PresentSegment<>(elems);
        tree = tree.caseEmpty().unify(
                emptyTree -> emptyTree.append(seg),
                nonEmptyTree -> nonEmptyTree.split(Stats::getSize, position).map((l, m, r) -> {
                    return join(l, m, seg, m, r);
                }));
    }

    public void insertVoid(int position, int length) {
        if(length < 0) {
            throw new IllegalArgumentException(
                    "length cannot be negative: " + length);
        } else if(length == 0) {
            return;
        }

        AbsentSegment<E> seg = new AbsentSegment<>(length);
        tree = tree.caseEmpty().unify(
                emptyTree -> emptyTree.append(seg),
                nonEmptyTree -> nonEmptyTree.split(Stats::getSize, position).map((l, m, r) -> {
                    return join(l, m, seg, m, r);
                }));
    }

    public void splice(int from, int to, Collection<? extends E> elems) {
        if(elems.isEmpty()) {
            remove(from, to);
        } else if(from == to) {
            insertAll(from, elems);
        } else {
            spliceSegments(
                    from, to,
                    Collections.singletonList(new PresentSegment<>(elems)));
        }
    }

    public void spliceByVoid(int from, int to, int length) {
        if(length == 0) {
            remove(from, to);
        } else if(length < 0) {
            throw new IllegalArgumentException(
                    "length cannot be negative: " + length);
        } else if(from == to) {
            insertVoid(from, length);
        } else {
            spliceSegments(
                    from, to,
                    Collections.singletonList(new AbsentSegment<>(length)));
        }
    }

    private void spliceSegments(int from, int to, List<Segment<E>> middle) {
        Lists.checkRange(from, to, tree.getStats().getSize());
        tree = tree.caseEmpty()
                .mapLeft(emptyTree -> join(emptyTree, middle, emptyTree))
                .toLeft(nonEmptyTree -> nonEmptyTree.split(Stats::getSize, from).map((left, lSuffix, r) -> {
                    return nonEmptyTree.split(Stats::getSize, to).map((l, rPrefix, right) -> {
                        return join(left, lSuffix, middle, rPrefix, right);
                    });
                }));
    }

    private FingerTree<Segment<E>, Stats> join(
            FingerTree<Segment<E>, Stats> left,
            Tuple2<Segment<E>, Integer> lSuffix,
            Segment<E> middle,
            Tuple2<Segment<E>, Integer> rPrefix,
            FingerTree<Segment<E>, Stats> right) {
        return join(
                left, lSuffix,
                Collections.singletonList(middle),
                rPrefix, right);
    }

    private FingerTree<Segment<E>, Stats> join(
            FingerTree<Segment<E>, Stats> left,
            Tuple2<Segment<E>, Integer> lSuffix,
            List<Segment<E>> middle,
            Tuple2<Segment<E>, Integer> rPrefix,
            FingerTree<Segment<E>, Stats> right) {

        Segment<E> lSeg = lSuffix._1;
        int lMax = lSuffix._2;
        if(lMax > 0) {
            left = left.append(lSeg.subSegment(0, lMax));
        }

        Segment<E> rSeg = rPrefix._1;
        int rMin = rPrefix._2;
        if(rMin < rSeg.getLength()) {
            right = right.prepend(rSeg.subSegment(rMin, rSeg.getLength()));
        }

        return join(left, middle, right);
    }

    private FingerTree<Segment<E>, Stats> join(
            FingerTree<Segment<E>, Stats> left,
            List<Segment<E>> middle,
            FingerTree<Segment<E>, Stats> right) {

        for(Segment<E> seg: middle) {
            left = append(left, seg);
        }
        return join(left, right);
    }

    private FingerTree<Segment<E>, Stats> join(
            FingerTree<Segment<E>, Stats> left,
            FingerTree<Segment<E>, Stats> right) {
        if(left.isEmpty()) {
            return right;
        } else if(right.isEmpty()) {
            return left;
        } else {
            Segment<E> lastLeft = left.getLeaf(left.getLeafCount() - 1);
            Segment<E> firstRight = right.getLeaf(0);
            if(lastLeft.possiblyDestructiveAppend(firstRight)) {
                left = left.updateLeaf(left.getLeafCount() - 1, lastLeft);
                right = right.split(1)._2;
            }
            return left.join(right);
        }
    }

    private FingerTree<Segment<E>, Stats> append(
            FingerTree<Segment<E>, Stats> left,
            Segment<E> right) {
        if(left.isEmpty()) {
            return left.append(right);
        } else {
            Segment<E> lastLeft = left.getLeaf(left.getLeafCount() - 1);
            if(lastLeft.possiblyDestructiveAppend(right)) {
                return left.updateLeaf(left.getLeafCount() - 1, lastLeft);
            } else {
                return left.append(right);
            }
        }
    }

    /**
     * For testing only.
     */
    int getDepth() {
        return tree.getDepth();
    }

    /**
     * For testing only.
     * @return
     */
    FingerTree<Segment<E>, Stats> getTree() {
        return tree;
    }
}