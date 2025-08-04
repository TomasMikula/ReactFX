package org.reactfx.collection;

import java.util.List;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import org.reactfx.Subscription;
import org.reactfx.util.Lists;
import org.reactfx.value.Val;

/**
 * The mapped list is applying a mapping {@link Function} taking as input the element from the source method
 * and mapping the result to be used for the mapped list content
 * @param <E> the type of elements from the source
 * @param <F> the output type of the {@link Function} mapping the elements
 */
class MappedList<E, F> extends AbstractMappedList<E, F> {
    private final Function<? super E, ? extends F> mapper;

    public MappedList(
            ObservableList<? extends E> source,
            Function<? super E, ? extends F> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    public F get(int index) {
        return mapper.apply(getSource().get(index));
    }

    @Override
    protected void sourceChanged(QuasiListChange<? extends E> change) {
        notifyObservers(mappedChangeView(change, mapper));
    }

    static <E, F> QuasiListChange<F> mappedChangeView(
            QuasiListChange<? extends E> change,
            Function<? super E, ? extends F> mapper) {
        return new QuasiListChange<F>() {

            @Override
            public List<? extends QuasiListModification<? extends F>> getModifications() {
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
                        return Lists.mappedView(mod.getRemoved(), mapper);
                    }
                });
            }

        };
    }
}

class DynamicallyMappedList<E, F> extends LiveListBase<F>
implements UnmodifiableByDefaultLiveList<F> {
    private final ObservableList<? extends E> source;
    private final Val<? extends Function<? super E, ? extends F>> mapper;

    public DynamicallyMappedList(
            ObservableList<? extends E> source,
            ObservableValue<? extends Function<? super E, ? extends F>> mapper) {
        this.source = source;
        this.mapper = Val.wrap(mapper);
    }

    @Override
    public F get(int index) {
        return mapper.getValue().apply(source.get(index));
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    protected Subscription observeInputs() {
        return Subscription.multi(
                LiveList.<E>observeQuasiChanges(source, this::sourceChanged),
                mapper.observeInvalidations(this::mapperInvalidated));
    }

    private void sourceChanged(QuasiListChange<? extends E> change) {
        notifyObservers(MappedList.mappedChangeView(change, mapper.getValue()));
    }

    private void mapperInvalidated(Function<? super E, ? extends F> oldMapper) {
        fireContentReplacement(Lists.mappedView(source, oldMapper));
    }
}