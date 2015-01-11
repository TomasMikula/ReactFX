package org.reactfx.collection;

import org.reactfx.ObservableBase;
import org.reactfx.util.NotificationAccumulator;

abstract class ObsListBase<E>
extends ObservableBase<ObsList.Observer<? super E, ?>, QuasiListChange<? extends E>>
implements ObsListHelpers<E>, AccessorListMethods<E> {

    public ObsListBase() {
        super(NotificationAccumulator.listNotifications());
    }
}
