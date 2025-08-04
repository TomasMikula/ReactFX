package org.reactfx.collection;

import javafx.collections.ObservableList;
import org.reactfx.util.Lists;

import java.util.List;
import java.util.function.BiFunction;

/**
 * The index mapped list is applying a {@link BiFunction} taking as input the index of the element and the source element
 * itself and return a mapping used for the mapped list content
 * @param <E> the type of elements from the source
 * @param <F> the output type of the {@link BiFunction} mapping the elements
 */
class IndexedMappedList<E, F> extends AbstractMappedList<E, F> {
    private final BiFunction<Integer, ? super E, ? extends F> mapper;

    public IndexedMappedList(ObservableList<? extends E> source,
                             BiFunction<Integer, ? super E, ? extends F> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    protected F apply(int index, E elem) {
        return mapper.apply(index, elem);
    }

    @Override
    protected void sourceChanged(QuasiListChange<? extends E> change) {
        notifyObservers(mappedChangeView(change));
    }

    private QuasiListChange<F> mappedChangeView(QuasiListChange<? extends E> change) {
        return () -> {
            List<? extends QuasiListModification<? extends E>> mods = change.getModifications();
            return Lists.<QuasiListModification<? extends E>, QuasiListModification<F>>mappedView(mods, mod -> new QuasiListModification<F>() {

                @Override
                public int getFrom() {
                    return mod.getFrom();
                }

                @Override
                public int getAddedSize() {
                    return mod.getAddedSize();
                }

                @Override
                public List<? extends F> getRemoved() {
                    return Lists.mappedView(mod.getRemoved(), elem -> mapper.apply(mod.getFrom(), elem));
                }
            });
        };
    }
}