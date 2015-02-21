package org.reactfx.collection;

import org.reactfx.ObservableBase;

public abstract class LiveListBase<E>
extends ObservableBase<LiveList.Observer<? super E, ?>, QuasiListChange<? extends E>>
implements ProperLiveList<E>, AccessorListMethods<E> {}
