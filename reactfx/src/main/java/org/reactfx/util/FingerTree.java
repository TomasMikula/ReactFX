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

    private abstract class Node {

        abstract int getDepth();
        abstract int getLeafCount();
        abstract T getLeaf(int index);
        abstract Node updateLeaf(int index, T data);
        abstract T getData(); // valid for leafs only
        abstract S getStats();

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

        abstract Tuple2<Node, Node> split(int beforeLeaf);

        abstract Tuple3<Node, Optional<Tuple2<T, Integer>>, Node> split(
                ToIntFunction<? super S> metric,
                int position);

        final Node append(Node right) {
            if(this.getDepth() >= right.getDepth()) {
                return appendLte(right).toLeft(two -> two.map(Branch::new));
            } else {
                return right.prepend(this);
            }
        }

        final Node prepend(Node left) {
            if(this.getDepth() >= left.getDepth()) {
                return prependLte(left).toLeft(two -> two.map(Branch::new));
            } else {
                return left.append(this);
            }
        }

        abstract Either<Node, Tuple2<Node, Node>> appendLte(Node right);
        abstract Either<Node, Tuple2<Node, Node>> prependLte(Node left);
    }

    private final class EmptyNode extends Node {

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
        Node updateLeaf(int index, T data) {
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
        Tuple2<Node, Node> split(
                int beforeLeaf) {
            assert beforeLeaf == 0;
            return t(this, this);
        }

        @Override
        Tuple3<Node, Optional<Tuple2<T, Integer>>, Node> split(
                ToIntFunction<? super S> metric, int position) {
            assert position == 0;
            return t(this, Optional.empty(), this);
        }

        @Override
        Either<Node, Tuple2<Node, Node>> appendLte(Node right) {
            assert right.getDepth() == 0;
            return left(right);
        }

        @Override
        Either<Node, Tuple2<Node, Node>> prependLte(Node left) {
            assert left.getDepth() == 0;
            return left(left);
        }
    }

    private class Leaf extends Node {
        private final T data;
        private final S stats;

        Leaf(T data) {
            this.data = data;
            this.stats = leafStats.apply(data);
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
        Node updateLeaf(int index, T data) {
            assert index == 0;
            return new Leaf(data);
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
        Either<Node, Tuple2<Node, Node>> appendLte(Node right) {
            assert right.getDepth() <= this.getDepth();
            if(right.getDepth() == 0) {
                return left(this);
            } else {
                return right(t(this, right));
            }
        }

        @Override
        Either<Node, Tuple2<Node, Node>> prependLte(Node left) {
            assert left.getDepth() <= this.getDepth();
            if(left.getDepth() == 0) {
                return left(this);
            } else {
                return right(t(left, this));
            }
        }

        @Override
        Tuple2<Node, Node> split(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, 1);
            if(beforeLeaf == 0) {
                return t(EMPTY_NODE, this);
            } else {
                return t(this, EMPTY_NODE);
            }
        }

        @Override
        Tuple3<Node, Optional<Tuple2<T, Integer>>, Node> split(
                ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            if(position == 0) {
                return t(EMPTY_NODE, Optional.empty(), this);
            } else if(position == measure(metric)) {
                return t(this, Optional.empty(), EMPTY_NODE);
            } else {
                return t(EMPTY_NODE, Optional.of(t(data, position)), EMPTY_NODE);
            }
        }
    }

    private final class Branch extends Node {
        private final Cons<Node> children;
        private final int depth;
        private final int leafCount;
        private final S stats;

        private Branch(Cons<Node> children) {
            assert children.size() == 2 || children.size() == 3;
            Node head = children.head();
            int headDepth = head.getDepth();
            assert children.all(n -> n.getDepth() == headDepth);
            this.children = children;
            this.depth  = 1 + headDepth;
            this.leafCount = children.fold(0, (s, n) -> s + n.getLeafCount());
            this.stats = children.mapReduce1(
                    Node::getStats,
                    monoid::reduce);
        }

        Branch(Node left, Node right) {
            this(LL.of(left, right));
        }

        Branch(Node left, Node middle, Node right) {
            this(LL.of(left, middle, right));
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

        private T getLeaf(int index, LL<? extends Node> nodes) {
            Node head = nodes.head();
            int headSize = head.getLeafCount();
            if(index < headSize) {
                return head.getLeaf(index);
            } else {
                return getLeaf(index - headSize, nodes.tail());
            }
        }

        @Override
        Node updateLeaf(int index, T data) {
            assert Lists.isValidIndex(index, getLeafCount());
            return new Branch(updateLeaf(index, data, children));
        }

        private Cons<Node> updateLeaf(int index, T data, LL<? extends Node> nodes) {
            Node head = nodes.head();
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
                LL<? extends Node> nodes) {
            Node head = nodes.head();
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
                LL<? extends Node> nodes) {
            Node head = nodes.head();
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
                LL<? extends Node> nodes) {
            Node head = nodes.head();
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
                LL<? extends Node> nodes) {
            Node head = nodes.head();
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
                LL<? extends Node> nodes) {
            Node head = nodes.head();
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
        Either<Node, Tuple2<Node, Node>> appendLte(Node suffix) {
            assert suffix.getDepth() <= this.getDepth();
            if(suffix.getDepth() == this.getDepth()) {
                return right(t(this, suffix));
            } else if(children.size() == 2) {
                return children.mapFirst2((left, right) -> {
                    return right.appendLte(suffix).unify(
                            r -> left(new Branch(left, r)),
                            mr -> left(mr.map((m, r) -> new Branch(left, m, r))));
                });
            } else {
                assert children.size() == 3;
                return children.mapFirst3((left, middle, right) -> {
                    return right.appendLte(suffix).<Either<Node, Tuple2<Node, Node>>>unify(
                            r -> left(new Branch(left, middle, r)),
                            mr -> right(t(new Branch(left, middle), mr.map(Branch::new))));
                });
            }
        }

        @Override
        Either<Node, Tuple2<Node, Node>> prependLte(Node prefix) {
            assert prefix.getDepth() <= this.getDepth();
            if(prefix.getDepth() == this.getDepth()) {
                return right(t(prefix, this));
            } else if(children.size() == 2) {
                return children.mapFirst2((left, right) -> {
                    return left.prependLte(prefix).unify(
                            l -> left(new Branch(l, right)),
                            lm -> left(lm.map((l, m) -> new Branch(l, m, right))));
                });
            } else {
                assert children.size() == 3;
                return children.mapFirst3((left, middle, right) -> {
                    return left.prependLte(prefix).<Either<Node, Tuple2<Node, Node>>>unify(
                            l -> left(new Branch(l, middle, right)),
                            lm -> right(t(lm.map(Branch::new), new Branch(middle, right))));
                });
            }
        }

        @Override
        Tuple2<Node, Node> split(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, getLeafCount());
            if(beforeLeaf == 0) {
                return t(EMPTY_NODE, this);
            } else {
                return split(beforeLeaf, children);
            }
        }

        private Tuple2<Node, Node> split(
                int beforeLeaf, LL<? extends Node> nodes) {
            assert beforeLeaf > 0;
            Node head = nodes.head();
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
        Tuple3<Node, Optional<Tuple2<T, Integer>>, Node> split(
                ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            if(position == 0) {
                return t(EMPTY_NODE, Optional.empty(), this);
            } else {
                return split(metric, position, children);
            }
        }

        private Tuple3<Node, Optional<Tuple2<T, Integer>>, Node> split(
                ToIntFunction<? super S> metric,
                int position,
                LL<? extends Node> nodes) {
            assert position > 0;
            Node head = nodes.head();
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
            Function<? super T, ? extends S> leafStats,
            Monoid<S> monoid) {
        return new FingerTree<>(leafStats, monoid);
    }

    private final EmptyNode EMPTY_NODE = new EmptyNode();

    private final Function<? super T, ? extends S> leafStats;
    private final Monoid<S> monoid;
    private final Node root;

    private FingerTree(
            Function<? super T, ? extends S> leafStats,
            Monoid<S> monoid) {
        this.leafStats = leafStats;
        this.monoid = monoid;
        this.root = EMPTY_NODE;
    }

    private FingerTree(
            Function<? super T, ? extends S> leafStats,
            Monoid<S> monoid,
            Node root) {
        this.leafStats = leafStats;
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
            Node left = root.split(fromLeaf)._1;
            Node right = root.split(toLeaf)._2;
            return newTree(left.append(right));
        }
    }

    public FingerTree<T, S> insertLeaf(int position, T data) {
        Lists.checkPosition(position, getLeafCount());
        return newTree(
                root.split(position)
                        .map((l, r) -> l.append(new Leaf(data)).append(r)));
    }

    public FingerTree<T, S> updateLeaf(int index, T data) {
        Lists.checkIndex(index, getLeafCount());
        return newTree(root.updateLeaf(index, data));
    }

    public FingerTree<T, S> append(T data) {
        return newTree(root.append(new Leaf(data)));
    }

    public FingerTree<T, S> prepend(T data) {
        return newTree(root.prepend(new Leaf(data)));
    }

    public FingerTree<T, S> join(FingerTree<T, S> rightTree) {
        return newTree(root.append(rightTree.root));
    }

    private FingerTree<T, S> newTree(Node newRoot) {
        return new FingerTree<>(leafStats, monoid, newRoot);
    }

    private FingerTree<T, S> newEmptyTree() {
        return newEmptyTree(leafStats, monoid);
    }

    private Node concat(Cons<? extends Node> nodes) {
        Node head = nodes.head();
        return nodes.tail().fold(
                head,
                (v, w) -> v.append(w));
    }
}

interface Monoid<T> {
    T unit();
    T reduce(T left, T right);
}

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