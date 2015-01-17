package org.reactfx.collection;

import org.reactfx.ObservableBase;
import org.reactfx.util.NotificationAccumulator;

abstract class LiveListBase<E>
extends ObservableBase<LiveList.Observer<? super E, ?>, QuasiListChange<? extends E>>
implements LiveListHelpers<E>, AccessorListMethods<E> {

    public LiveListBase() {
        super(NotificationAccumulator.listNotifications());
    }
}
