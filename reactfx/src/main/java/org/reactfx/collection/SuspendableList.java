package org.reactfx.collection;

import org.reactfx.Guard;
import org.reactfx.Suspendable;

@SuppressWarnings("deprecation")
public interface SuspendableList<E>
extends LiveList<E>, Suspendable, org.reactfx.inhibeans.collection.ObservableList<E> {
    @Override @Deprecated
    default Guard block() { return suspend(); }
}