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

final class FingerTree<T, S> {

    private static abstract class Node<T, S> {
        final MapToMonoid<? super T, S> monoid;

        Node(MapToMonoid<? super T, S> monoid) {
            this.monoid = monoid;
        }

        abstract int getDepth();
        abstract int getLeafCount();
        abstract T getLeaf(int index);
        abstract Node<T, S> updateLeaf(int index, T data);
        abstract T getData(); // valid for leafs only
        abstract S getStats();

        EmptyNode<T, S> emptyNode() {
            return new EmptyNode<>(monoid);
        }

        Leaf<T, S> leaf(T data) {
            return new Leaf<>(monoid, data);
        }

        Branch<T, S> branch(Node<T, S> left, Node<T, S> right) {
            return branch(LL.of(left, right));
        }

        Branch<T, S> branch(Node<T, S> left, Node<T, S> middle, Node<T, S> right) {
            return branch(LL.of(left, middle, right));
        }

        Branch<T, S> branch(Cons<Node<T, S>> children) {
            return new Branch<>(monoid, children);
        }

        final boolean isEmpty() {
            return getDepth() == 0;
        }

        final int measure(ToIntFunction<? super S> metric) {
            return metric.applyAsInt(getStats());
        }

        abstract BiIndex locate(
                ToIntFunction<? super S> metric,
                int position);

        abstract <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction);

        abstract <R> R foldBetween(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf, int endLeaf);

        abstract <R> R foldBetween(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction);

        abstract S getStatsBetween(
                int startLeaf,
                int endLeaf);

        abstract S getStatsBetween(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats);

        abstract Tuple2<Node<T, S>, Node<T, S>> split(int beforeLeaf);

        abstract Tuple3<Node<T, S>, Optional<Tuple2<T, Integer>>, Node<T, S>> split(
                ToIntFunction<? super S> metric,
                int position);

        final Node<T, S> append(Node<T, S> right) {
            if(this.getDepth() >= right.getDepth()) {
                return appendLte(right).toLeft(two -> two.map(this::branch));
            } else {
                return right.prepend(this);
            }
        }

        final Node<T, S> prepend(Node<T, S> left) {
            if(this.getDepth() >= left.getDepth()) {
                return prependLte(left).toLeft(two -> two.map(this::branch));
            } else {
                return left.append(this);
            }
        }

        abstract Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> appendLte(Node<T, S> right);
        abstract Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> prependLte(Node<T, S> left);
    }

    private static final class EmptyNode<T, S> extends Node<T, S> {

        EmptyNode(MapToMonoid<? super T, S> monoid) {
            super(monoid);
        }

        @Override
        public String toString() {
            return"<emtpy>";
        }

        @Override
        int getDepth() {
            return 0;
        }

        @Override
        int getLeafCount() {
            return 0;
        }

        @Override
        T getLeaf(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        Node<T, S> updateLeaf(int index, T data) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        T getData() {
            throw new NoSuchElementException();
        }

        @Override
        S getStats() {
            return monoid.unit();
        }

        @Override
        BiIndex locate(ToIntFunction<? super S> metric, int position) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return acc;
        }

        @Override
        <R> R foldBetween(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf, int endLeaf) {
            assert Lists.isValidRange(startLeaf, endLeaf, 0);
            return acc;
        }

        @Override
        <R> R foldBetween(
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
        S getStatsBetween(int startLeaf, int endLeaf) {
            assert Lists.isValidRange(startLeaf, endLeaf, 0);
            return monoid.unit();
        }

        @Override
        S getStatsBetween(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats) {
            assert Lists.isValidRange(startPosition, endPosition, 0);
            return monoid.unit();
        }

        @Override
        Tuple2<Node<T, S>, Node<T, S>> split(
                int beforeLeaf) {
            assert beforeLeaf == 0;
            return t(this, this);
        }

        @Override
        Tuple3<Node<T, S>, Optional<Tuple2<T, Integer>>, Node<T, S>> split(
                ToIntFunction<? super S> metric, int position) {
            assert position == 0;
            return t(this, Optional.empty(), this);
        }

        @Override
        Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> appendLte(Node<T, S> right) {
            assert right.getDepth() == 0;
            return left(right);
        }

        @Override
        Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> prependLte(Node<T, S> left) {
            assert left.getDepth() == 0;
            return left(left);
        }
    }

    private static class Leaf<T, S> extends Node<T, S> {
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
        int getDepth() {
            return 1;
        }

        @Override
        int getLeafCount() {
            return 1;
        }

        @Override
        T getLeaf(int index) {
            assert index == 0;
            return data;
        }

        @Override
        Node<T, S> updateLeaf(int index, T data) {
            assert index == 0;
            return leaf(data);
        }

        @Override
        T getData() {
            return data;
        }

        @Override
        S getStats() {
            return stats;
        }

        @Override
        BiIndex locate(ToIntFunction<? super S> metric, int position) {
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
        <R> R foldBetween(
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
        <R> R foldBetween(
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
        S getStatsBetween(int startLeaf, int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount())
                    : "Didn't expect empty range [" + startLeaf + ", " + endLeaf + ")";
            return getStats();
        }

        @Override
        S getStatsBetween(
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
        Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> appendLte(Node<T, S> right) {
            assert right.getDepth() <= this.getDepth();
            if(right.getDepth() == 0) {
                return left(this);
            } else {
                return right(t(this, right));
            }
        }

        @Override
        Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> prependLte(Node<T, S> left) {
            assert left.getDepth() <= this.getDepth();
            if(left.getDepth() == 0) {
                return left(this);
            } else {
                return right(t(left, this));
            }
        }

        @Override
        Tuple2<Node<T, S>, Node<T, S>> split(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, 1);
            if(beforeLeaf == 0) {
                return t(emptyNode(), this);
            } else {
                return t(this, emptyNode());
            }
        }

        @Override
        Tuple3<Node<T, S>, Optional<Tuple2<T, Integer>>, Node<T, S>> split(
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

    private static final class Branch<T, S> extends Node<T, S> {
        private final Cons<Node<T, S>> children;
        private final int depth;
        private final int leafCount;
        private final S stats;

        private Branch(MapToMonoid<? super T, S> monoid, Cons<Node<T, S>> children) {
            super(monoid);
            assert children.size() == 2 || children.size() == 3;
            Node<T, S> head = children.head();
            int headDepth = head.getDepth();
            assert children.all(n -> n.getDepth() == headDepth);
            this.children = children;
            this.depth  = 1 + headDepth;
            this.leafCount = children.fold(0, (s, n) -> s + n.getLeafCount());
            this.stats = children.mapReduce1(
                    Node<T, S>::getStats,
                    monoid::reduce);
        }

        @Override
        public String toString() {
            return "Branch" + children;
        }

        @Override
        final int getDepth() {
            return depth;
        }

        @Override
        final int getLeafCount() {
            return leafCount;
        }

        @Override
        final T getData() {
            throw new UnsupportedOperationException("Only leaf nodes hold data");
        }

        @Override
        final T getLeaf(int index) {
            assert Lists.isValidIndex(index, getLeafCount());
            return getLeaf(index, children);
        }

        private T getLeaf(int index, LL<? extends Node<T, S>> nodes) {
            Node<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(index < headSize) {
                return head.getLeaf(index);
            } else {
                return getLeaf(index - headSize, nodes.tail());
            }
        }

        @Override
        Node<T, S> updateLeaf(int index, T data) {
            assert Lists.isValidIndex(index, getLeafCount());
            return branch(updateLeaf(index, data, children));
        }

        private Cons<Node<T, S>> updateLeaf(int index, T data, LL<? extends Node<T, S>> nodes) {
            Node<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(index < headSize) {
                return cons(head.updateLeaf(index, data), nodes.tail());
            } else {
                return cons(head, updateLeaf(index - headSize, data, nodes.tail()));
            }
        }

        @Override
        final BiIndex locate(ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            return locate(metric, position, children);
        }

        private BiIndex locate(
                ToIntFunction<? super S> metric,
                int position,
                LL<? extends Node<T, S>> nodes) {
            Node<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            if(position < headLen || position == headLen && nodes.tail().isEmpty()) {
                return head.locate(metric, position);
            } else {
                return locate(metric, position - headLen, nodes.tail())
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
        final <R> R foldBetween(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf,
                int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount());
            return foldBetween(acc, reduction, startLeaf, endLeaf, children);
        }

        private <R> R foldBetween(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf,
                int endLeaf,
                LL<? extends Node<T, S>> nodes) {
            Node<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            int headTo = Math.min(endLeaf, headSize);
            int tailFrom = Math.max(startLeaf - headSize, 0);
            int tailTo = endLeaf - headSize;
            if(startLeaf < headTo) {
                acc = head.foldBetween(acc, reduction, startLeaf, headTo);
            }
            if(tailFrom < tailTo) {
                acc = foldBetween(acc, reduction, tailFrom, tailTo, nodes.tail());
            }
            return acc;
        }

        @Override
        final <R> R foldBetween(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
            assert Lists.isNonEmptyRange(startPosition, endPosition, measure(metric));
            return foldBetween(acc, reduction, metric, startPosition, endPosition, rangeReduction, children);
        }

        private <R> R foldBetween(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction,
                LL<? extends Node<T, S>> nodes) {
            Node<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            int headTo = Math.min(endPosition, headLen);
            int tailFrom = Math.max(startPosition - headLen, 0);
            int tailTo = endPosition - headLen;
            if(startPosition < headTo) {
                acc = head.foldBetween(acc, reduction, metric, startPosition, headTo, rangeReduction);
            }
            if(tailFrom < tailTo) {
                acc = foldBetween(acc, reduction, metric, tailFrom, tailTo, rangeReduction, nodes.tail());
            }
            return acc;
        }

        @Override
        final S getStats() {
            return stats;
        }

        @Override
        final S getStatsBetween(
                int startLeaf,
                int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount());
            if(startLeaf == 0 && endLeaf == getLeafCount()) {
                return getStats();
            } else {
                return getStatsBetween(startLeaf, endLeaf, children);
            }
        }

        private S getStatsBetween(
                int startLeaf,
                int endLeaf,
                LL<? extends Node<T, S>> nodes) {
            Node<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            int headTo = Math.min(endLeaf, headSize);
            int tailFrom = Math.max(startLeaf - headSize, 0);
            int tailTo = endLeaf - headSize;
            if(startLeaf < headTo && tailFrom < tailTo) {
                return monoid.reduce(
                        head.getStatsBetween(startLeaf, headTo),
                        getStatsBetween(tailFrom, tailTo, nodes.tail()));
            } else if(startLeaf < headTo) {
                return head.getStatsBetween(startLeaf, headTo);
            } else if(tailFrom < tailTo) {
                return getStatsBetween(tailFrom, tailTo, nodes.tail());
            } else {
                throw new AssertionError("Didn't expect empty range: [" + startLeaf + ", " + endLeaf + ")");
            }
        }

        @Override
        final S getStatsBetween(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats) {
            int len = measure(metric);
            assert Lists.isNonEmptyRange(startPosition, endPosition, len);
            if(startPosition == 0 && endPosition == len) {
                return getStats();
            } else {
                return getStatsBetween(metric, startPosition, endPosition, subStats, children);
            }
        }

        private S getStatsBetween(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subStats,
                LL<? extends Node<T, S>> nodes) {
            Node<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            int headTo = Math.min(endPosition, headLen);
            int tailFrom = Math.max(startPosition - headLen, 0);
            int tailTo = endPosition - headLen;
            if(startPosition < headTo && tailFrom < tailTo) {
                return monoid.reduce(
                        head.getStatsBetween( metric, startPosition, headTo, subStats),
                        getStatsBetween(metric, tailFrom, tailTo, subStats, nodes.tail()));
            } else if(startPosition < headTo) {
                return head.getStatsBetween(metric, startPosition, headTo, subStats);
            } else if(tailFrom < tailTo) {
                return getStatsBetween(metric, tailFrom, tailTo, subStats, nodes.tail());
            } else {
                throw new AssertionError("Didn't expect empty range: [" + startPosition + ", " + endPosition + ")");
            }
        }

        @Override
        Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> appendLte(Node<T, S> suffix) {
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
                    return right.appendLte(suffix).<Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>>>unify(
                            r -> left(branch(left, middle, r)),
                            mr -> right(t(branch(left, middle), mr.map(this::branch))));
                });
            }
        }

        @Override
        Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>> prependLte(Node<T, S> prefix) {
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
                    return left.prependLte(prefix).<Either<Node<T, S>, Tuple2<Node<T, S>, Node<T, S>>>>unify(
                            l -> left(branch(l, middle, right)),
                            lm -> right(t(lm.map(this::branch), branch(middle, right))));
                });
            }
        }

        @Override
        Tuple2<Node<T, S>, Node<T, S>> split(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, getLeafCount());
            if(beforeLeaf == 0) {
                return t(emptyNode(), this);
            } else {
                return split(beforeLeaf, children);
            }
        }

        private Tuple2<Node<T, S>, Node<T, S>> split(
                int beforeLeaf, LL<? extends Node<T, S>> nodes) {
            assert beforeLeaf > 0;
            Node<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(beforeLeaf <= headSize) {
                return head.split(beforeLeaf)
                        .map((l, r) -> t(l, concat(cons(r, nodes.tail()))));
            } else {
                return split(beforeLeaf - headSize, nodes.tail())
                        .map((l, r) -> t(head.append(l), r));
            }
        }

        @Override
        Tuple3<Node<T, S>, Optional<Tuple2<T, Integer>>, Node<T, S>> split(
                ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            if(position == 0) {
                return t(emptyNode(), Optional.empty(), this);
            } else {
                return split(metric, position, children);
            }
        }

        private Tuple3<Node<T, S>, Optional<Tuple2<T, Integer>>, Node<T, S>> split(
                ToIntFunction<? super S> metric,
                int position,
                LL<? extends Node<T, S>> nodes) {
            assert position > 0;
            Node<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            if(position <= headLen) {
                return head.split(metric, position)
                        .map((l, m, r) -> t(l, m, concat(cons(r, nodes.tail()))));
            } else {
                return split(metric, position - headLen, nodes.tail())
                        .map((l, m, r) -> t(head.append(l), m, r));
            }
        }
    }

    public static <T, S> FingerTree<T, S> newEmptyTree(
            MapToMonoid<? super T, S> monoid) {
        return new FingerTree<>(monoid);
    }

    private final MapToMonoid<? super T, S> monoid;
    private final Node<T, S> root;

    private FingerTree(
            MapToMonoid<? super T, S> monoid) {
        this.monoid = monoid;
        this.root = new EmptyNode<>(monoid);
    }

    private FingerTree(
            MapToMonoid<? super T, S> monoid,
            Node<T, S> root) {
        this.monoid = monoid;
        this.root = root;
    }

    @Override
    public String toString() {
        return "FingerTree[" + root + "]";
    }

    public boolean isEmpty() {
        return root.isEmpty();
    }

    public int getLeafCount() {
        return root.getLeafCount();
    }

    public int getDepth() {
        return root.getDepth();
    }

    public T getLeaf(int index) {
        Lists.checkIndex(index, getLeafCount());
        return root.getLeaf(index);
    }

    public BiIndex locate(
            ToIntFunction<? super S> metric,
            int position) {

        if(getLeafCount() == 0) {
            throw new IndexOutOfBoundsException("no leafs to locate in");
        }

        Lists.checkPosition(position, root.measure(metric));
        return root.locate(metric, position);
    }

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

    public <R> R fold(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction) {
        return root.fold(acc, reduction);
    }

    public <R> R foldBetween(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            int startLeaf,
            int endLeaf) {
        Lists.checkRange(startLeaf, endLeaf, getLeafCount());
        return root.foldBetween(acc, reduction, startLeaf, endLeaf);
    }

    public <R> R foldBetween(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
        Lists.checkRange(startPosition, endPosition, root.measure(metric));
        return root.foldBetween(
                acc, reduction, metric, startPosition, endPosition, rangeReduction);
    }

    public S getStats() {
        return root.getStats();
    }

    public S getStatsBetween(int startLeaf, int endLeaf) {
        Lists.checkRange(startLeaf, endLeaf, getLeafCount());
        return root.getStatsBetween(startLeaf, endLeaf);
    }

    public S getStatsBetween(
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TriFunction<? super T, Integer, Integer, ? extends S> subStats) {
        Lists.checkRange(startPosition, endPosition, root.measure(metric));
        return root.getStatsBetween(metric, startPosition, endPosition, subStats);
    }

    public Tuple2<FingerTree<T, S>, FingerTree<T, S>> split(int beforeLeaf) {
        Lists.checkPosition(beforeLeaf, getLeafCount());
        return root.split(beforeLeaf)
                .map((l, r) -> t(newTree(l), newTree(r)));
    }

    public Tuple3<FingerTree<T, S>, Optional<Tuple2<T, Integer>>, FingerTree<T, S>> split(
            ToIntFunction<? super S> metric, int position) {
        Lists.checkPosition(position, root.measure(metric));
        return root.split(metric, position)
                .map((l, t, r) -> t(newTree(l), t, newTree(r)));
    }

    public FingerTree<T, S> removeLeafs(int fromLeaf, int toLeaf) {
        Lists.checkRange(fromLeaf, toLeaf, getLeafCount());
        if(fromLeaf == toLeaf) {
            return this;
        } else if(fromLeaf == 0 && toLeaf == getLeafCount()) {
            return newEmptyTree();
        } else {
            Node<T, S> left = root.split(fromLeaf)._1;
            Node<T, S> right = root.split(toLeaf)._2;
            return newTree(left.append(right));
        }
    }

    public FingerTree<T, S> insertLeaf(int position, T data) {
        Lists.checkPosition(position, getLeafCount());
        return newTree(
                root.split(position)
                        .map((l, r) -> l.append(root.leaf(data)).append(r)));
    }

    public FingerTree<T, S> updateLeaf(int index, T data) {
        Lists.checkIndex(index, getLeafCount());
        return newTree(root.updateLeaf(index, data));
    }

    public FingerTree<T, S> append(T data) {
        return newTree(root.append(root.leaf(data)));
    }

    public FingerTree<T, S> prepend(T data) {
        return newTree(root.prepend(root.leaf(data)));
    }

    public FingerTree<T, S> join(FingerTree<T, S> rightTree) {
        return newTree(root.append(rightTree.root));
    }

    private FingerTree<T, S> newTree(Node<T, S> newRoot) {
        return new FingerTree<>(monoid, newRoot);
    }

    private FingerTree<T, S> newEmptyTree() {
        return newEmptyTree(monoid);
    }

    private static <T, S> Node<T, S> concat(Cons<? extends Node<T, S>> nodes) {
        Node<T, S> head = nodes.head();
        return nodes.tail().fold(
                head,
                (v, w) -> v.append(w));
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