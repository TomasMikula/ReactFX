package org.reactfx.util;

import static org.reactfx.util.Either.*;
import static org.reactfx.util.LL.*;
import static org.reactfx.util.Tuples.*;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.reactfx.util.LL.Cons;

abstract class FingerTree<T, S> {

    private static final class Empty<T, S> extends FingerTree<T, S> {

        Empty(MapToMonoid<? super T, S> monoid) {
            super(monoid);
        }

        @Override
        public String toString() {
            return"<emtpy tree>";
        }

        @Override
        public
        int getDepth() {
            return 0;
        }

        @Override
        public
        int getLeafCount() {
            return 0;
        }

        @Override
        T getLeaf0(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        FingerTree<T, S> updateLeaf0(int index, T data) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        T getData() {
            throw new NoSuchElementException();
        }

        @Override
        public
        S getStats() {
            return monoid.unit();
        }

        @Override
        BiIndex locate0(ToIntFunction<? super S> metric, int position) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return acc;
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf, int endLeaf) {
            assert Lists.isValidRange(startLeaf, endLeaf, 0);
            return acc;
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
            assert Lists.isValidRange(startPosition, endPosition, 0);
            return acc;
        }

        @Override
        S getStatsBetween0(int startLeaf, int endLeaf) {
            assert Lists.isValidRange(startLeaf, endLeaf, 0);
            return monoid.unit();
        }

        @Override
        S getStatsBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats) {
            assert Lists.isValidRange(startPosition, endPosition, 0);
            return monoid.unit();
        }

        @Override
        Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(
                int beforeLeaf) {
            assert beforeLeaf == 0;
            return t(this, this);
        }

        @Override
        Tuple3<FingerTree<T, S>, Optional<Tuple2<T, Integer>>, FingerTree<T, S>> split0(
                ToIntFunction<? super S> metric, int position) {
            assert position == 0;
            return t(this, Optional.empty(), this);
        }

        @Override
        Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> appendLte(FingerTree<T, S> right) {
            assert right.getDepth() == 0;
            return left(right);
        }

        @Override
        Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> prependLte(FingerTree<T, S> left) {
            assert left.getDepth() == 0;
            return left(left);
        }
    }

    private static class Leaf<T, S> extends FingerTree<T, S> {
        private final T data;
        private final S stats;

        Leaf(MapToMonoid<? super T, S> monoid, T data) {
            super(monoid);
            this.data = data;
            this.stats = monoid.apply(data);
        }

        @Override
        public String toString() {
            return "Leaf(" + data + ")";
        }

        @Override
        public int getDepth() {
            return 1;
        }

        @Override
        public int getLeafCount() {
            return 1;
        }

        @Override
        T getLeaf0(int index) {
            assert index == 0;
            return data;
        }

        @Override
        FingerTree<T, S> updateLeaf0(int index, T data) {
            assert index == 0;
            return leaf(data);
        }

        @Override
        T getData() {
            return data;
        }

        @Override
        public S getStats() {
            return stats;
        }

        @Override
        BiIndex locate0(ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            return new BiIndex(0, position);
        }

        @Override
        <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return reduction.apply(acc, data);
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf, int endLeaf) {

            assert 0 <= startLeaf;
            assert endLeaf <= 1;

            if(startLeaf < endLeaf) {
                return reduction.apply(acc, data);
            } else {
                return acc;
            }
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {

            assert Lists.isValidRange(startPosition, endPosition, measure(metric));
            return rangeReduction.apply(acc, data, startPosition, endPosition);
        }

        @Override
        S getStatsBetween0(int startLeaf, int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount())
                    : "Didn't expect empty range [" + startLeaf + ", " + endLeaf + ")";
            return getStats();
        }

        @Override
        S getStatsBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats) {

            assert Lists.isNonEmptyRange(startPosition, endPosition, measure(metric))
                    : "Didn't expect empty range [" + startPosition + ", " + endPosition + ")";

            int len = measure(metric);
            if(startPosition == 0 && endPosition == len) {
                return stats;
            } else {
                return subStats.apply(data, startPosition, endPosition);
            }
        }

        @Override
        Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> appendLte(FingerTree<T, S> right) {
            assert right.getDepth() <= this.getDepth();
            if(right.getDepth() == 0) {
                return left(this);
            } else {
                return right(t(this, right));
            }
        }

        @Override
        Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> prependLte(FingerTree<T, S> left) {
            assert left.getDepth() <= this.getDepth();
            if(left.getDepth() == 0) {
                return left(this);
            } else {
                return right(t(left, this));
            }
        }

        @Override
        Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, 1);
            if(beforeLeaf == 0) {
                return t(emptyNode(), this);
            } else {
                return t(this, emptyNode());
            }
        }

        @Override
        Tuple3<FingerTree<T, S>, Optional<Tuple2<T, Integer>>, FingerTree<T, S>> split0(
                ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            if(position == 0) {
                return t(emptyNode(), Optional.empty(), this);
            } else if(position == measure(metric)) {
                return t(this, Optional.empty(), emptyNode());
            } else {
                return t(emptyNode(), Optional.of(t(data, position)), emptyNode());
            }
        }
    }

    private static final class Branch<T, S> extends FingerTree<T, S> {
        private final Cons<FingerTree<T, S>> children;
        private final int depth;
        private final int leafCount;
        private final S stats;

        private Branch(MapToMonoid<? super T, S> monoid, Cons<FingerTree<T, S>> children) {
            super(monoid);
            assert children.size() == 2 || children.size() == 3;
            FingerTree<T, S> head = children.head();
            int headDepth = head.getDepth();
            assert children.all(n -> n.getDepth() == headDepth);
            this.children = children;
            this.depth  = 1 + headDepth;
            this.leafCount = children.fold(0, (s, n) -> s + n.getLeafCount());
            this.stats = children.mapReduce1(
                    FingerTree<T, S>::getStats,
                    monoid::reduce);
        }

        @Override
        public String toString() {
            return "Branch" + children;
        }

        @Override
        public int getDepth() {
            return depth;
        }

        @Override
        public int getLeafCount() {
            return leafCount;
        }

        @Override
        final T getData() {
            throw new UnsupportedOperationException("Only leaf nodes hold data");
        }

        @Override
        final T getLeaf0(int index) {
            assert Lists.isValidIndex(index, getLeafCount());
            return getLeaf0(index, children);
        }

        private T getLeaf0(int index, LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(index < headSize) {
                return head.getLeaf0(index);
            } else {
                return getLeaf0(index - headSize, nodes.tail());
            }
        }

        @Override
        FingerTree<T, S> updateLeaf0(int index, T data) {
            assert Lists.isValidIndex(index, getLeafCount());
            return branch(updateLeaf0(index, data, children));
        }

        private Cons<FingerTree<T, S>> updateLeaf0(int index, T data, LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(index < headSize) {
                return cons(head.updateLeaf0(index, data), nodes.tail());
            } else {
                return cons(head, updateLeaf0(index - headSize, data, nodes.tail()));
            }
        }

        @Override
        final BiIndex locate0(ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            return locate0(metric, position, children);
        }

        private BiIndex locate0(
                ToIntFunction<? super S> metric,
                int position,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            if(position < headLen || position == headLen && nodes.tail().isEmpty()) {
                return head.locate0(metric, position);
            } else {
                return locate0(metric, position - headLen, nodes.tail())
                        .adjustMajor(head.getLeafCount());
            }
        }

        @Override
        final <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return children.fold(acc, (r, n) -> n.fold(r, reduction));
        }

        @Override
        final <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf,
                int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount());
            return foldBetween0(acc, reduction, startLeaf, endLeaf, children);
        }

        private <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf,
                int endLeaf,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            int headTo = Math.min(endLeaf, headSize);
            int tailFrom = Math.max(startLeaf - headSize, 0);
            int tailTo = endLeaf - headSize;
            if(startLeaf < headTo) {
                acc = head.foldBetween0(acc, reduction, startLeaf, headTo);
            }
            if(tailFrom < tailTo) {
                acc = foldBetween0(acc, reduction, tailFrom, tailTo, nodes.tail());
            }
            return acc;
        }

        @Override
        final <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
            assert Lists.isNonEmptyRange(startPosition, endPosition, measure(metric));
            return foldBetween0(acc, reduction, metric, startPosition, endPosition, rangeReduction, children);
        }

        private <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            int headTo = Math.min(endPosition, headLen);
            int tailFrom = Math.max(startPosition - headLen, 0);
            int tailTo = endPosition - headLen;
            if(startPosition < headTo) {
                acc = head.foldBetween0(acc, reduction, metric, startPosition, headTo, rangeReduction);
            }
            if(tailFrom < tailTo) {
                acc = foldBetween0(acc, reduction, metric, tailFrom, tailTo, rangeReduction, nodes.tail());
            }
            return acc;
        }

        @Override
        public S getStats() {
            return stats;
        }

        @Override
        final S getStatsBetween0(
                int startLeaf,
                int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount());
            if(startLeaf == 0 && endLeaf == getLeafCount()) {
                return getStats();
            } else {
                return getStatsBetween0(startLeaf, endLeaf, children);
            }
        }

        private S getStatsBetween0(
                int startLeaf,
                int endLeaf,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            int headTo = Math.min(endLeaf, headSize);
            int tailFrom = Math.max(startLeaf - headSize, 0);
            int tailTo = endLeaf - headSize;
            if(startLeaf < headTo && tailFrom < tailTo) {
                return monoid.reduce(
                        head.getStatsBetween0(startLeaf, headTo),
                        getStatsBetween0(tailFrom, tailTo, nodes.tail()));
            } else if(startLeaf < headTo) {
                return head.getStatsBetween0(startLeaf, headTo);
            } else if(tailFrom < tailTo) {
                return getStatsBetween0(tailFrom, tailTo, nodes.tail());
            } else {
                throw new AssertionError("Didn't expect empty range: [" + startLeaf + ", " + endLeaf + ")");
            }
        }

        @Override
        final S getStatsBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats) {
            int len = measure(metric);
            assert Lists.isNonEmptyRange(startPosition, endPosition, len);
            if(startPosition == 0 && endPosition == len) {
                return getStats();
            } else {
                return getStatsBetween0(metric, startPosition, endPosition, subStats, children);
            }
        }

        private S getStatsBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            int headTo = Math.min(endPosition, headLen);
            int tailFrom = Math.max(startPosition - headLen, 0);
            int tailTo = endPosition - headLen;
            if(startPosition < headTo && tailFrom < tailTo) {
                return monoid.reduce(
                        head.getStatsBetween0( metric, startPosition, headTo, subStats),
                        getStatsBetween0(metric, tailFrom, tailTo, subStats, nodes.tail()));
            } else if(startPosition < headTo) {
                return head.getStatsBetween0(metric, startPosition, headTo, subStats);
            } else if(tailFrom < tailTo) {
                return getStatsBetween0(metric, tailFrom, tailTo, subStats, nodes.tail());
            } else {
                throw new AssertionError("Didn't expect empty range: [" + startPosition + ", " + endPosition + ")");
            }
        }

        @Override
        Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> appendLte(FingerTree<T, S> suffix) {
            assert suffix.getDepth() <= this.getDepth();
            if(suffix.getDepth() == this.getDepth()) {
                return right(t(this, suffix));
            } else if(children.size() == 2) {
                return children.mapFirst2((left, right) -> {
                    return right.appendLte(suffix).unify(
                            r -> left(branch(left, r)),
                            mr -> left(mr.map((m, r) -> branch(left, m, r))));
                });
            } else {
                assert children.size() == 3;
                return children.mapFirst3((left, middle, right) -> {
                    return right.appendLte(suffix).<Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>>>unify(
                            r -> left(branch(left, middle, r)),
                            mr -> right(t(branch(left, middle), mr.map(this::branch))));
                });
            }
        }

        @Override
        Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> prependLte(FingerTree<T, S> prefix) {
            assert prefix.getDepth() <= this.getDepth();
            if(prefix.getDepth() == this.getDepth()) {
                return right(t(prefix, this));
            } else if(children.size() == 2) {
                return children.mapFirst2((left, right) -> {
                    return left.prependLte(prefix).unify(
                            l -> left(branch(l, right)),
                            lm -> left(lm.map((l, m) -> branch(l, m, right))));
                });
            } else {
                assert children.size() == 3;
                return children.mapFirst3((left, middle, right) -> {
                    return left.prependLte(prefix).<Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>>>unify(
                            l -> left(branch(l, middle, right)),
                            lm -> right(t(lm.map(this::branch), branch(middle, right))));
                });
            }
        }

        @Override
        Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, getLeafCount());
            if(beforeLeaf == 0) {
                return t(emptyNode(), this);
            } else {
                return split0(beforeLeaf, children);
            }
        }

        private Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(
                int beforeLeaf, LL<? extends FingerTree<T, S>> nodes) {
            assert beforeLeaf > 0;
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(beforeLeaf <= headSize) {
                return head.split0(beforeLeaf)
                        .map((l, r) -> t(l, concat(cons(r, nodes.tail()))));
            } else {
                return split0(beforeLeaf - headSize, nodes.tail())
                        .map((l, r) -> t(head.appendTree(l), r));
            }
        }

        @Override
        Tuple3<FingerTree<T, S>, Optional<Tuple2<T, Integer>>, FingerTree<T, S>> split0(
                ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            if(position == 0) {
                return t(emptyNode(), Optional.empty(), this);
            } else {
                return split0(metric, position, children);
            }
        }

        private Tuple3<FingerTree<T, S>, Optional<Tuple2<T, Integer>>, FingerTree<T, S>> split0(
                ToIntFunction<? super S> metric,
                int position,
                LL<? extends FingerTree<T, S>> nodes) {
            assert position > 0;
            FingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            if(position <= headLen) {
                return head.split0(metric, position)
                        .map((l, m, r) -> t(l, m, concat(cons(r, nodes.tail()))));
            } else {
                return split0(metric, position - headLen, nodes.tail())
                        .map((l, m, r) -> t(head.appendTree(l), m, r));
            }
        }
    }

    public static <T, S> FingerTree<T, S> emptyTree(
            MapToMonoid<? super T, S> monoid) {
        return new Empty<>(monoid);
    }

    private static <T, S> FingerTree<T, S> concat(Cons<? extends FingerTree<T, S>> nodes) {
        FingerTree<T, S> head = nodes.head();
        return nodes.tail().fold(
                head,
                (v, w) -> v.appendTree(w));
    }

    final MapToMonoid<? super T, S> monoid;

    private FingerTree(MapToMonoid<? super T, S> monoid) {
        this.monoid = monoid;
    }

    public abstract int getDepth();
    public abstract int getLeafCount();
    public abstract S getStats();

    public final boolean isEmpty() {
        return getDepth() == 0;
    }

    public T getLeaf(int index) {
        Lists.checkIndex(index, getLeafCount());
        return getLeaf0(index);
    }

    abstract T getLeaf0(int index);

    public Tuple2<T, BiIndex> get(
            ToIntFunction<? super S> metric,
            int index) {
        int size = metric.applyAsInt(getStats());
        Lists.checkIndex(index, size);
        BiIndex location = locate(metric, index);
        return t(getLeaf(location.major), location);
    }

    public <E> E get(
            ToIntFunction<? super S> metric,
            int index,
            BiFunction<? super T, Integer, E> leafAccessor) {
        return locate(metric, index)
                .map((major, minor) -> leafAccessor.apply(getLeaf(major), minor));
    }

    public FingerTree<T, S> updateLeaf(int index, T data) {
        Lists.checkIndex(index, getLeafCount());
        return updateLeaf0(index, data);
    }

    abstract FingerTree<T, S> updateLeaf0(int index, T data);
    public BiIndex locate(
            ToIntFunction<? super S> metric,
            int position) {
    
        if(getLeafCount() == 0) {
            throw new IndexOutOfBoundsException("no leafs to locate in");
        }
    
        Lists.checkPosition(position, measure(metric));
        return locate0(metric, position);
    }

    abstract BiIndex locate0(
            ToIntFunction<? super S> metric,
            int position);

    abstract <R> R fold(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction);

    public <R> R foldBetween(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            int startLeaf,
            int endLeaf) {
        Lists.checkRange(startLeaf, endLeaf, getLeafCount());
        return foldBetween0(acc, reduction, startLeaf, endLeaf);
    }

    abstract <R> R foldBetween0(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            int startLeaf, int endLeaf);

    public <R> R foldBetween(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
        Lists.checkRange(startPosition, endPosition, measure(metric));
        return foldBetween0(
                acc, reduction, metric, startPosition, endPosition, rangeReduction);
    }

    abstract <R> R foldBetween0(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction);

    public S getStatsBetween(int startLeaf, int endLeaf) {
        Lists.checkRange(startLeaf, endLeaf, getLeafCount());
        return getStatsBetween0(startLeaf, endLeaf);
    }

    abstract S getStatsBetween0(
            int startLeaf,
            int endLeaf);

    public S getStatsBetween(
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TriFunction<? super T, Integer, Integer, ? extends S> subStats) {
        Lists.checkRange(startPosition, endPosition, measure(metric));
        return getStatsBetween0(metric, startPosition, endPosition, subStats);
    }

    abstract S getStatsBetween0(
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TriFunction<? super T, Integer, Integer, ? extends S> subStats);

    public Tuple2<FingerTree<T, S>, FingerTree<T, S>> split(int beforeLeaf) {
        Lists.checkPosition(beforeLeaf, getLeafCount());
        return split0(beforeLeaf);
    }

    abstract Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(int beforeLeaf);

    public Tuple3<FingerTree<T, S>, Optional<Tuple2<T, Integer>>, FingerTree<T, S>> split(
            ToIntFunction<? super S> metric, int position) {
        Lists.checkPosition(position, measure(metric));
        return split0(metric, position);
    }

    abstract Tuple3<FingerTree<T, S>, Optional<Tuple2<T, Integer>>, FingerTree<T, S>> split0(
            ToIntFunction<? super S> metric,
            int position);

    public FingerTree<T, S> removeLeafs(int fromLeaf, int toLeaf) {
        Lists.checkRange(fromLeaf, toLeaf, getLeafCount());
        if(fromLeaf == toLeaf) {
            return this;
        } else if(fromLeaf == 0 && toLeaf == getLeafCount()) {
            return emptyNode();
        } else {
            FingerTree<T, S> left = split0(fromLeaf)._1;
            FingerTree<T, S> right = split0(toLeaf)._2;
            return left.appendTree(right);
        }
    }

    public FingerTree<T, S> insertLeaf(int position, T data) {
        Lists.checkPosition(position, getLeafCount());
        return split0(position)
                .map((l, r) -> l.appendTree(leaf(data)).appendTree(r));
    }

    public FingerTree<T, S> join(FingerTree<T, S> rightTree) {
        return appendTree(rightTree);
    }

    final FingerTree<T, S> appendTree(FingerTree<T, S> right) {
        if(this.getDepth() >= right.getDepth()) {
            return appendLte(right).toLeft(two -> two.map(this::branch));
        } else {
            return right.prependTree(this);
        }
    }

    final FingerTree<T, S> prependTree(FingerTree<T, S> left) {
        if(this.getDepth() >= left.getDepth()) {
            return prependLte(left).toLeft(two -> two.map(this::branch));
        } else {
            return left.appendTree(this);
        }
    }

    abstract Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> appendLte(FingerTree<T, S> right);
    abstract Either<FingerTree<T, S>, Tuple2<FingerTree<T, S>, FingerTree<T, S>>> prependLte(FingerTree<T, S> left);


    public FingerTree<T, S> append(T data) {
        return appendTree(leaf(data));
    }

    public FingerTree<T, S> prepend(T data) {
        return prependTree(leaf(data));
    }

    abstract T getData(); // valid for leafs only

    Empty<T, S> emptyNode() {
        return new Empty<>(monoid);
    }

    Leaf<T, S> leaf(T data) {
        return new Leaf<>(monoid, data);
    }

    Branch<T, S> branch(FingerTree<T, S> left, FingerTree<T, S> right) {
        return branch(LL.of(left, right));
    }

    Branch<T, S> branch(FingerTree<T, S> left, FingerTree<T, S> middle, FingerTree<T, S> right) {
        return branch(LL.of(left, middle, right));
    }

    Branch<T, S> branch(Cons<FingerTree<T, S>> children) {
        return new Branch<>(monoid, children);
    }

    final int measure(ToIntFunction<? super S> metric) {
        return metric.applyAsInt(getStats());
    }
}

interface Monoid<T> {
    T unit();
    T reduce(T left, T right);
}

interface MapToMonoid<T, U> extends Function<T, U>, Monoid<U> {}

final class BiIndex {
    public final int major;
    public final int minor;

    public BiIndex(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public <T> T map(BiFunction<Integer, Integer, T> f) {
        return f.apply(major, minor);
    }

    public BiIndex adjustMajor(int adjustment) {
        return new BiIndex(major + adjustment, minor);
    }

    public BiIndex adjustMinor(int adjustment) {
        return new BiIndex(major, minor + adjustment);
    }
}