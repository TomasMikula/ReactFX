package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import org.reactfx.util.Lists;

/**
 * Stores a list of {@link QuasiListModification}s
 */
public interface QuasiListChange<E> extends ListModificationSequence<E> {

    @Override
    default QuasiListChange<E> asListChange() {
        return this;
    }

    @Override
    default ListChangeAccumulator<E> asListChangeAccumulator() {
        return new ListChangeAccumulator<>(this);
    }

    @SuppressWarnings("unchecked")
    static <E> QuasiListChange<E> safeCast(
            QuasiListChange<? extends E> mod) {
        // the cast is safe, because instances are immutable
        return (QuasiListChange<E>) mod;
    }

    /**
     * Creates a QuasiListChange based on the given {@code change} that only holds {@link QuasiListModification}s
     * in its list of {@link #getModifications()}.
     */
    static <E> QuasiListChange<E> from(Change<? extends E> ch) {
        QuasiListChangeImpl<E> res = new QuasiListChangeImpl<>();
        while(ch.next()) {
            res.add(QuasiListModification.fromCurrentStateOf(ch));
        }
        return res;
    }

    static <E> ListChange<E> instantiate(
            QuasiListChange<? extends E> change,
            ObservableList<E> list) {
        return () -> Lists.<QuasiListModification<? extends E>, ListModification<? extends E>>mappedView(
                change.getModifications(),
                mod -> QuasiListModification.instantiate(mod, list));
    }
}

@SuppressWarnings("serial")
final class QuasiListChangeImpl<E>
extends ArrayList<QuasiListModification<? extends E>>
implements QuasiListChange<E> {

    public QuasiListChangeImpl() {
        super();
    }

    public QuasiListChangeImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public QuasiListChangeImpl(QuasiListChange<E> change) {
        super(change.getModifications());
    }

    @Override
    public List<QuasiListModification<? extends E>> getModifications() {
        return Collections.unmodifiableList(this);
    }
}