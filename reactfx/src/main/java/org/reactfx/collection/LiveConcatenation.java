package org.reactfx.collection;

import java.util.Arrays;
import java.util.List;

import javafx.collections.ObservableList;

import org.reactfx.Subscription;
import org.reactfx.util.Lists;

public class LiveConcatenation<E>
extends LiveListBase<E>
implements ReadOnlyLiveListImpl<E> {

    private final ObservableList<? extends E> left;

    private final ObservableList<? extends E> right;

    public LiveConcatenation(
        ObservableList<? extends E> left,
        ObservableList<? extends E> right) {
        this.left = left;
        this.right = right;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <E> LiveList<E> multi(
        ObservableList<? extends E>... inputs) {
        switch (inputs.length) {
            case 0:
                return EmptyLiveList.getInstance();
            case 1:
                return new LiveListWrapper<>(inputs[0]);
            case 2:
                return new LiveConcatenation<>(inputs[0], inputs[1]);
            case 3:
                return new LiveConcatenation<>(
                    new LiveConcatenation<>(inputs[0], inputs[1]),
                    inputs[2]
                );
            default:
                return multi(
                    new LiveConcatenation<>(inputs[0], inputs[1]),
                    multi(Arrays.stream(inputs).skip(2).toArray(ObservableList[]::new))
                );
        }
    }

    @Override
    protected Subscription observeInputs() {
        return Subscription.multi(
            LiveList.observeQuasiChanges(left, this::notifyObservers),
            LiveList.observeQuasiChanges(
                right,
                change -> notifyObservers(change, left.size())
            )
        );
    }

    private void notifyObservers(
        QuasiListChange<? extends E> change,
        int offset) {
        notifyObservers(() -> Lists.mappedView(change.getModifications(),
            mod -> new QuasiListModification<E>() {
                @Override
                public int getFrom() {
                    return mod.getFrom() + offset;
                }

                @Override
                public int getAddedSize() {
                    return mod.getAddedSize();
                }

                @Override
                public List<? extends E> getRemoved() {
                    return mod.getRemoved();
                }
            }
        ));
    }

    @Override
    public int size() {
        return left.size() + right.size();
    }

    @Override
    public E get(
        int index) {
        int offset = left.size();
        return index < offset
            ? left.get(index)
            : right.get(index - offset);
    }
}
