package org.reactfx.collection;

import java.util.function.BinaryOperator;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;

import org.reactfx.Subscription;
import org.reactfx.util.Experimental;
import org.reactfx.util.FingerTree;
import org.reactfx.util.MapToMonoid;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;

class ListReduction<T> extends ValBase<T> {
    private final ObservableList<T> input;
    private final BinaryOperator<T> reduction;
    private final MapToMonoid<T, T> monoid;

    private FingerTree<T, T> tree = null;

    ListReduction(
            ObservableList<T> input,
            BinaryOperator<T> reduction) {
        this.input = input;
        this.reduction = reduction;
        monoid = new MapToMonoid<T, T>() {

            @Override
            public T apply(T t) {
                return t;
            }

            @Override
            public T unit() {
                return null;
            }

            @Override
            public T reduce(T left, T right) {
                return reduction.apply(left, right);
            }
        };
    }

    @Override
    protected Subscription connect() {
        assert tree == null;
        tree = FingerTree.mkTree(input, monoid);
        return LiveList.observeChanges(input, ch -> {
            for(ListModification<? extends T> mod: ch) {
                FingerTree<T, T> left = tree.split(mod.getFrom())._1;
                FingerTree<T, T> right = tree.split(mod.getFrom() + mod.getRemovedSize())._2;
                FingerTree<T, T> middle = FingerTree.mkTree(mod.getAddedSubList(), monoid);
                tree = left.join(middle).join(right);
            }
            invalidate();
        })
        .and(() -> tree = null);
    }

    protected final T computeReduction(int from, int to) {
        if(isObservingInputs()) {
            assert tree != null;
            return tree.getStatsBetween(from, to);
        } else {
            assert tree == null;
            return input.subList(from, to).stream().reduce(reduction).orElse(null);
        }
    }

    @Override
    protected T computeValue() {
        return computeReduction(0, input.size());
    }

    protected final int listSize() {
        return input.size();
    }
}


@Experimental
class ListRangeReduction<T> extends ListReduction<T> {
    private final ObservableValue<IndexRange> range;

    ListRangeReduction(
            ObservableList<T> input,
            ObservableValue<IndexRange> range,
            BinaryOperator<T> reduction) {
        super(input, reduction);
        this.range = range;
    }

    @Override
    protected Subscription connect() {
        return super.connect().and(Val.observeInvalidations(range, obs -> invalidate()));
    }

    @Override
    protected T computeValue() {
        int from = Math.min(range.getValue().getStart(), listSize());
        int to = Math.min(range.getValue().getEnd(), listSize());
        return computeReduction(from, to);
    }
}