package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/**
 * Sub-interface of {@link ListModificationLike} that does not provide a way to get the added elements directly,
 * but has to be combined with the observable list to obtain {@linkplain ListModification}
 * ({@link QuasiListModification#instantiate(QuasiListModification, ObservableList)}) or
 * {@linkplain MaterializedListModification} ({@link QuasiListModification#materialize(QuasiListModification, ObservableList)}).
 */
public interface QuasiListModification<E> extends ListModificationLike<E> {

    /**
     * Creates a {@link QuasiListModification} using the given parameters
     */
    static <E> QuasiListModification<E> create(
            int position,
            List<? extends E> removed,
            int addedSize) {
        return new QuasiListModificationImpl<>(position, removed, addedSize);
    }

    /**
     * Creates a {@link QuasiListModification} from the current state in the
     * {@link javafx.collections.ListChangeListener.Change}.
     */
    static <E, F extends E> QuasiListModification<E> fromCurrentStateOf(
            Change<F> ch) {
        List<F> list = ch.getList();
        int from = ch.getFrom();
        int addedSize = ch.getTo() - from; // use (to - from), because
                                           // ch.getAddedSize() is 0 on permutation

        List<F> removed;
        if(ch.wasPermutated()) {
            removed = new ArrayList<>(addedSize);
            for(int i = 0; i < addedSize; ++i) {
                int pi = ch.getPermutation(from + i);
                removed.add(list.get(pi));
            }
        } else {
            removed = ch.getRemoved();
        }
        return new QuasiListModificationImpl<>(from, removed, addedSize);
    }

    static <E> ListModification<E> instantiate(
            QuasiListModification<? extends E> template,
            ObservableList<E> list) {
        return new ListModificationImpl<>(
                template.getFrom(),
                template.getRemoved(),
                template.getAddedSize(),
                list);
    }

    static <E> MaterializedListModification<E> materialize(
            QuasiListModification<? extends E> template,
            ObservableList<E> list) {
        return MaterializedListModification.create(
                template.getFrom(),
                template.getRemoved(),
                new ArrayList<>(list.subList(template.getFrom(), template.getTo())));
    }

    default ListModification<E> instantiate(ObservableList<E> list) {
        return instantiate(this, list);
    }

    default MaterializedListModification<E> materialize(ObservableList<E> list) {
        return materialize(this, list);
    }

    default QuasiListChange<E> asListChange() {
        return () -> Collections.singletonList(this);
    }
}

class QuasiListModificationImpl<E> implements QuasiListModification<E> {

    private final int position;
    private final List<? extends E> removed;
    private final int addedSize;

    QuasiListModificationImpl(
            int position,
            List<? extends E> removed,
            int addedSize) {
        this.position = position;
        this.removed = Collections.unmodifiableList(removed);
        this.addedSize = addedSize;
    }

    @Override
    public int getFrom() {
        return position;
    }

    @Override
    public int getAddedSize() {
        return addedSize;
    }

    @Override
    public List<? extends E> getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return "[modification at: " + getFrom() +
                ", removed: " + getRemoved() +
                ", added size: " + getAddedSize() + "]";
    }
}