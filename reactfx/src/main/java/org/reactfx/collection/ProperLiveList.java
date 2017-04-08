package org.reactfx.collection;

import java.util.Collections;
import java.util.List;

import org.reactfx.ObservableBase;
import org.reactfx.ProperObservable;
import org.reactfx.util.Lists;
import org.reactfx.util.NotificationAccumulator;

/**
 * Trait to be mixed into {@link ObservableBase} to obtain default
 * implementation of some {@link LiveList} methods and get additional
 * helper methods for implementations of <em>proper</em> {@linkplain LiveList}.
 *
 * <p>
 *     It adds methods for quickly notifying observers of insertions, removals, or replacements via
 *     {@code fire}-related methods.<br>
 *     It specifies that the {@link #defaultNotificationAccumulator()} will be
 *     {@link NotificationAccumulator#listNotifications()} unless overridden in other cases.<br>
 *     It also properly implements default list methods that can be used for {@link Object#toString()},
 *     {@link Object#equals(Object)}, and {@link Object#hashCode()}.
 * </p>
 */
public interface ProperLiveList<E>
extends LiveList<E>, ProperObservable<LiveList.Observer<? super E, ?>, QuasiListChange<? extends E>> {

    /**
     * Returns {@link NotificationAccumulator#listNotifications()}
     */
    @Override
    default NotificationAccumulator<LiveList.Observer<? super E, ?>, QuasiListChange<? extends E>, ?> defaultNotificationAccumulator() {
        return NotificationAccumulator.listNotifications();
    }

    /**
     * Notifies observers of the given {@link QuasiListModification} as a {@link ListChange}
     */
    default void fireModification(QuasiListModification<? extends E> mod) {
        notifyObservers(mod.asListChange());
    }

    /**
     * Creates a {@link QuasiListModification} whose single item at index has been replaced by one item
     */
    static <E> QuasiListModification<E> elemReplacement(int index, E replaced) {
        return new QuasiListModificationImpl<E>(
                index, Collections.singletonList(replaced), 1);
    }

    /**
     * Calls {@link #fireModification(QuasiListModification)} using {@link #elemReplacement(int, Object)}
     */
    default void fireElemReplacement(int index, E replaced) {
        fireModification(elemReplacement(index, replaced));
    }

    /**
     * Creates a {@link QuasiListModification} that specifies the entire list's content has been replaced
     */
    default QuasiListModification<E> contentReplacement(List<E> removed) {
        return new QuasiListModificationImpl<E>(0, removed, size());
    }

    /**
     * Calls {@link #fireModification(QuasiListModification)} using {@link #contentReplacement(List)} where
     * {@code removed} is the list parameter.
     */
    default void fireContentReplacement(List<E> removed) {
        fireModification(contentReplacement(removed));
    }

    /**
     * Creates a {@link QuasiListModification} that specifies a single item has been added and nothing removed.
     */
    static <E> QuasiListModification<E> elemInsertion(int index) {
        return rangeInsertion(index, 1);
    }

    /**
     * Calls {@link #fireModification(QuasiListModification)} using {@link #elemInsertion(int)}
     */
    default void fireElemInsertion(int index) {
        fireModification(elemInsertion(index));
    }

    /**
     * Creates a {@link QuasiListModification} that specifies that content has been added and nothing added
     */
    static <E> QuasiListModification<E> rangeInsertion(int index, int size) {
        return new QuasiListModificationImpl<E>(
                index, Collections.emptyList(), size);
    }

    /**
     * Calls {@link #fireModification(QuasiListModification)} using {@link #rangeInsertion(int, int)}
     */
    default void fireRangeInsertion(int index, int size) {
        fireModification(rangeInsertion(index, size));
    }

    /**
     * Creates a {@link QuasiListModification} that specifies a single item has been removed and nothing added.
     */
    static <E> QuasiListModification<E> elemRemoval(int index, E removed) {
        return new QuasiListModificationImpl<E>(
                index, Collections.singletonList(removed), 0);
    }

    /**
     * Calls {@link #fireModification(QuasiListModification)} using {@link #elemRemoval(int, Object)}
     */
    default void fireElemRemoval(int index, E removed) {
        fireModification(elemRemoval(index, removed));
    }

    /**
     * Creates a {@link QuasiListModification} that specifies that content has been removed and nothing added.
     */
    static <E> QuasiListModification<E> rangeRemoval(int index, List<E> removed) {
        return new QuasiListModificationImpl<E>(index, removed, 0);
    }

    /**
     * Calls {@link #fireModification(QuasiListModification)} using {@link #rangeRemoval(int, List)}
     */
    default void fireRemoveRange(int index, List<E> removed) {
        fireModification(rangeRemoval(index, removed));
    }

    @Override
    default int defaultHashCode() {
        return Lists.hashCode(this);
    }

    @Override
    default boolean defaultEquals(Object o) {
        return Lists.equals(this, o);
    }

    @Override
    default String defaultToString() {
        return Lists.toString(this);
    }
}
