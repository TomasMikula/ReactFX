package org.reactfx.collection;

import org.reactfx.ObservableBase;
import org.reactfx.collection.ObsList.ChangeObserver;
import org.reactfx.util.AccuMap;
import org.reactfx.util.Lists;

abstract class ObsListBase<E>
extends ObservableBase<ChangeObserver<? super E>, ListChange<? extends E>>
implements ObsList<E> {

    public ObsListBase() {
        super(AccuMap.emptyListChangeAccumulationMap());
    }

    @Override
    public void addChangeObserver(ChangeObserver<? super E> listener) {
        addObserver(listener);
    }

    @Override
    public void removeChangeObserver(ChangeObserver<? super E> listener) {
        removeObserver(listener);
    }

    @Override
    public int hashCode() {
        return Lists.hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return Lists.equals(this, o);
    }

    @Override
    public String toString() {
        return Lists.toString(this);
    }

    @Override
    protected final boolean runUnsafeAction(Runnable action) {
        action.run();
        return true;
    }

    protected final void fireChange(ListChange<? extends E> change) {
        notifyObservers(ChangeObserver::onChange, change);
    }
}
