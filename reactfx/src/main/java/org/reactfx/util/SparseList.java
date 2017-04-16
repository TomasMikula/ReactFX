package org.reactfx.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javafx.scene.control.IndexRange;

/**
 * A list that observes a {@code sourceList} and stores information on which items in that {@code sourceList} are
 * considered "present" and which are considered "absent." The "present" items do not need to be continuous, but
 * can be split apart by items that are "absent."
 */
public final class SparseList<E> {

    /**
     * Represents a span in the {@code sourceList} with additional methods.
     */
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

    /**
     * Represents a span in the {@code sourceList} that is not considered "present" in the enclosing SparseList.
     */
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

    /**
     * Represents a span in the {@code sourceList} that is considered "present" in the enclosing SparseList.
     */
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

    /**
     * Calculates the size of this {@link SparseList}'s {@code sourceList} (size) is and the number of items
     * within that {@code sourceList} that are currently present in the {@code SparseList} (presentCount).
     */
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

    private static final ToSemigroup<Segment<?>, Stats> SEGMENT_STATS =
            new ToSemigroup<Segment<?>, Stats>() {

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

    /**
     * Gets the size of the {@code sourceList}
     */
    public int size() {
        return tree.getSummary(Stats.ZERO).size;
    }

    /**
     * Gets the size of the "present" items
     */
    public int getPresentCount() {
        return tree.getSummary(Stats.ZERO).presentCount;
    }

    /**
     * Returns true if the item at the index in the {@code sourceList}'s index system is included in the list
     * of "present" items.
     */
    public boolean isPresent(int index) {
        return tree.get(Stats::getSize, index, Segment::isPresent);
    }

    public E getOrThrow(int index) {
        return tree.get(Stats::getSize, index, Segment::getOrThrow);
    }

    /**
     * Gets the item at the {@code sourceList}'s index.
     */
    public Optional<E> get(int index) {
        return tree.get(Stats::getSize, index, Segment::get);
    }

    /**
     * Gets the item using the index system of the "present" items where 0 is the first present item even if
     * it's in the middle or end of the {@code sourceList}.
     */
    public E getPresent(int presentIndex) {
        return tree.get(
                Stats::getPresentCount,
                presentIndex,
                Segment::getOrThrow);
    }

    /**
     * Gets the number of items that are "present" before the given position.
     */
    public int getPresentCountBefore(int position) {
        Lists.checkPosition(position, size());
        return tree.getSummaryBetween(
                Stats::getSize,
                0, position,
                Segment::getStatsBetween).orElse(Stats.ZERO).getPresentCount();
    }

    /**
     * Gets the number of items that are "present" after the given position.
     */
    public int getPresentCountAfter(int position) {
        return getPresentCount() - getPresentCountBefore(position);
    }

    /**
     * Gets the number of items that are "present" between the given positions.
     */
    public int getPresentCountBetween(int from, int to) {
        Lists.checkRange(from, to, size());
        return getPresentCountBefore(to) - getPresentCountBefore(from);
    }

    /**
     * Returns the index (in the {@code sourceList}'s index system) of the "present" item using the index system of the
     * list of "present" items.
     */
    public int indexOfPresentItem(int presentIndex) {
        Lists.checkIndex(presentIndex, getPresentCount());
        return tree.locateProgressively(Stats::getPresentCount, presentIndex)
                .map(this::locationToPosition);
    }

    /**
     * Gets the lower and upper index bounds (in the {@code sourceList}'s index system) of the "present" items
     */
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
        return tree.getSummaryBetween(0, major).orElse(Stats.ZERO).size + minor;
    }

    /**
     * Returns a list of the "present" items.
     */
    public List<E> collect() {
        List<E> acc = new ArrayList<E>(getPresentCount());
        return tree.fold(acc, (l, seg) -> seg.appendTo(l));
    }

    /**
     * Returns a list of the "present" items between {@code from} and {@code to} using the index system
     * of the "present" items list.
     */
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

    /**
     * Clears out the list of "present" items.
     */
    public void clear() {
        tree = emptyTree();
    }

    /**
     * Counts the item at index as "absent."
     */
    public void remove(int index) {
        remove(index, index + 1);
    }

    /**
     * Counts the items in the specified range as "absent."
     */
    public void remove(int from, int to) {
        Lists.checkRange(from, to, size());
        if(from != to) {
            spliceSegments(from, to, Collections.emptyList());
        }
    }

    /**
     * Sets the given item and index as "present"
     */
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

    /**
     * Returns false if the item at the given index in the {@code sourceList}'s index system is included
     * in the list of "present" items; if it isn't, sets the item at that index in the "present" items list
     * to {@code elem}, and returns true.
     */
    public boolean setIfAbsent(int index, E elem) {
        if(isPresent(index)) {
            return false;
        } else {
            set(index, elem);
            return true;
        }
    }

    /**
     * Calls {@link #insertAll(int, Collection)}
     */
    public void insert(int position, E elem) {
        insertAll(position, Collections.singleton(elem));
    }

    /**
     * Inserts the given items into the list of "present" items list
     */
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

    /**
     * Counts the span in the {@code sourceList} from {@code position} to {@code length} as "absent".
     */
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

    /**
     * Counts the given items in the given index range as "present." If {@code elems} is empty, counts the
     * the items in the specified index range as "absent."
     */
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

    /**
     * Counts the given items in the given index range as "absent." Throws {@link IllegalArgumentException} if
     * {@code length < 0}.
     */
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
        Lists.checkRange(from, to, tree.getSummary(Stats.ZERO).getSize());
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