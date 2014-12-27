package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

public interface ListChange<E> extends ListModificationSequence<E> {

    // the cast is safe, because ListChange is immutable
    @SuppressWarnings("unchecked")
    static <E> ListChange<E> safeCast(ListChange<? extends E> change) {
        return (ListChange<E>) change;
    }

    @Override
    List<TransientListModification<E>> getModifications();

    @Override
    default ListChange<E> asListChange() {
        return this;
    }

    @Override
    default ListChangeAccumulator<E> asListChangeAccumulator() {
        return new ListChangeAccumulator<>(this);
    }

    default Optional<ListChangeListener.Change<E>> toJavaFx() {
        List<TransientListModification<E>> modifications = getModifications();

        if(modifications.isEmpty()) {
            return Optional.empty();
        }

        /* Can change to ObservableList<? extends E> and remove unsafe cast
         * when https://javafx-jira.kenai.com/browse/RT-39683 is resolved. */
        @SuppressWarnings("unchecked")
        ObservableList<E> list = (ObservableList<E>) modifications.get(0).getList();

        return Optional.of(new ListChangeListener.Change<E>(list) {

            private int current = -1;

            @Override
            public int getFrom() {
                return modifications.get(current).getFrom();
            }

            @Override
            protected int[] getPermutation() {
                return new int[0]; // not a permutation
            }

            /* Can change to List<? extends E> and remove unsafe cast when
             * https://javafx-jira.kenai.com/browse/RT-39683 is resolved. */
            @Override
            @SuppressWarnings("unchecked")
            public List<E> getRemoved() {
                // cast is safe, because the list is unmodifiable
                return (List<E>) modifications.get(current).getRemoved();
            }

            @Override
            public int getTo() {
                return modifications.get(current).getTo();
            }

            @Override
            public boolean next() {
                if(current + 1 < modifications.size()) {
                    ++current;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void reset() {
                current = -1;
            }
        });
    }


    static <E> ListChange<E> from(Change<? extends E> ch) {
        ListChangeImpl<E> res = new ListChangeImpl<>();
        while(ch.next()) {
            res.add(TransientListModification.fromCurrentStateOf(ch));
        }
        return res;
    }
}

@SuppressWarnings("serial")
final class ListChangeImpl<E>
extends ArrayList<TransientListModification<E>>
implements ListChange<E> {

    public ListChangeImpl() {
        super();
    }

    public ListChangeImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public ListChangeImpl(ListChange<E> change) {
        super(change.getModifications());
    }

    @Override
    public List<TransientListModification<E>> getModifications() {
        return Collections.unmodifiableList(this);
    }
}