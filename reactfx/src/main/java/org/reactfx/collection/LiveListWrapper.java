package org.reactfx.collection;

import javafx.collections.ObservableList;
import org.reactfx.Subscription;

public class LiveListWrapper<E>
extends LiveListBase<E>
implements ReadOnlyLiveListImpl<E> {

    private final ObservableList<? extends E> delegate;

    public LiveListWrapper(
        ObservableList<? extends E> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected Subscription observeInputs() {
        return LiveList.observeQuasiChanges(delegate, this::notifyObservers);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public E get(
        int index) {
        return delegate.get(index);
    }
}
