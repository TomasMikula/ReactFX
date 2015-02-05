package org.reactfx.value;


/**
 * Writable value whose invalidation and change notifications can be
 * temporarily suspended. Multiple invalidations encountered while this
 * observable value was suspended will result in a single invalidation and
 * at most one change notification when resumed.
 */
public interface SuspendableVar<T> extends SuspendableVal<T>, Var<T> {

}
