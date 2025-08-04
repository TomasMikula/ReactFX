package org.reactfx.collection;

import javafx.collections.ObservableList;
import org.reactfx.Subscription;
import org.reactfx.util.Lists;

import java.util.List;

abstract class AbstractMappedList<E, F> extends LiveListBase<F> implements UnmodifiableByDefaultLiveList<F> {
    private final ObservableList<? extends E> source;

    public AbstractMappedList(ObservableList<? extends E> source) {
        this.source = source;
    }

    @Override
    public int size() {
        return source.size();
    }

    protected ObservableList<? extends E> getSource() {
        return source;
    }

    @Override
    protected Subscription observeInputs() {
        return LiveList.<E>observeQuasiChanges(source, this::sourceChanged);
    }

    abstract protected void sourceChanged(QuasiListChange<? extends E> change);
}
