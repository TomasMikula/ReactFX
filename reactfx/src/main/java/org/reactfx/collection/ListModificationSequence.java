package org.reactfx.collection;

import java.util.Iterator;
import java.util.List;

/**
 * Common supertype for {@link ListChange} and {@link ListChangeAccumulator}.
 *
 * @param <E> type of list elements
 */
public interface ListModificationSequence<E>
extends Iterable<TransientListModification<E>> {

    List<TransientListModification<E>> getModifications();

    /**
     * May be destructive for this object. Therefore, this object should not
     * be used after the call to this method, unless stated otherwise by the
     * implementing class/interface.
     */
    ListChange<E> asListChange();

    /**
     * May be destructive for this object. Therefore, this object should not
     * be used after the call to this method, unless stated otherwise by the
     * implementing class/interface.
     */
    ListChangeAccumulator<E> asListChangeAccumulator();

    @Override
    default Iterator<TransientListModification<E>> iterator() {
        return getModifications().iterator();
    }
}
