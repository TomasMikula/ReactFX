package org.reactfx.collection;

import java.util.List;
import java.util.function.Function;

import javafx.collections.ObservableList;

import org.reactfx.Subscription;
import org.reactfx.util.Lists;

class MappedList<E, F> extends ObsListBase<E> implements ReadOnlyObsListImpl<E> {
    private final ObservableList<? extends F> source;
    private final Function<? super F, ? extends E> mapper;

    public MappedList(ObservableList<? extends F> source, Function<? super F, ? extends E> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public E get(int index) {
        return mapper.apply(source.get(index));
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    protected Subscription observeInputs() {
        return ObsList.<F>observeQuasiChanges(source, this::sourceChanged);
    }

    private void sourceChanged(QuasiListChange<? extends F> change) {
        notifyObservers(new QuasiListChange<E>() {

            @Override
            public List<? extends QuasiListModification<? extends E>> getModifications() {
                List<? extends QuasiListModification<? extends F>> mods = change.getModifications();
                return Lists.<QuasiListModification<? extends F>, QuasiListModification<E>>mappedView(mods, mod -> new QuasiListModification<E>() {

                    @Override
                    public int getFrom() {
                        return mod.getFrom();
                    }

                    @Override
                    public int getAddedSize() {
                        return mod.getAddedSize();
                    }

                    @Override
                    public List<? extends E> getRemoved() {
                        return Lists.mappedView(mod.getRemoved(), mapper);
                    }
                });
            }

        });
    }
}