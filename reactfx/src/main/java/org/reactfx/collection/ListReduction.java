package org.reactfx.collection;

import java.util.function.BinaryOperator;

import javafx.collections.ObservableList;

import org.reactfx.Subscription;
import org.reactfx.util.FingerTree;
import org.reactfx.util.MapToMonoid;
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

    @Override
    protected T computeValue() {
        if(isObservingInputs()) {
            assert tree != null;
            return tree.getStats();
        } else {
            assert tree == null;
            return input.stream().reduce(reduction).orElse(null);
        }
    }
}
