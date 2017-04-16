package org.reactfx.collection;

import java.util.Iterator;
import java.util.List;

import org.reactfx.util.Lists;

/**
 * Common supertype for {@link QuasiListChange} and {@link ListChangeAccumulator} that has methods
 * for "casting" one type to another. Note: these casts might be destructive. See the javadoc of
 * {@link #asListChange()} and {@link #asListChangeAccumulator()}.
 *
 * @param <E> type of list elements
 */
public interface ListModificationSequence<E>
extends AbstractListModificationSequence<E, QuasiListModification<? extends E>> {

    /**
     * May be destructive for this object. Therefore, this object should not
     * be used after the call to this method, unless stated otherwise by the
     * implementing class/interface.
     */
    QuasiListChange<E> asListChange();

    /**
     * May be destructive for this object. Therefore, this object should not
     * be used after the call to this method, unless stated otherwise by the
     * implementing class/interface.
     */
    ListChangeAccumulator<E> asListChangeAccumulator();
}

/**
 * A sequence of {@link ListModificationLike list modifications}. One can get the list of the modifications via
 * {@link #getModifications()}, their {@link #getModificationCount() number of modifications}, and
 * {@link #iterator() iterate over those modifications}.
 */
interface AbstractListModificationSequence<E, M extends ListModificationLike<? extends E>>
extends Iterable<M> {

    /**
     * Gets the {@link ListModificationLike list modifications}
     */
    List<? extends M> getModifications();

    /**
     * Iterates over {@link #getModifications()}
     */
    @Override
    default Iterator<M> iterator() {
        return Lists.readOnlyIterator(getModifications());
    }

    default int getModificationCount() {
        return getModifications().size();
    }
}