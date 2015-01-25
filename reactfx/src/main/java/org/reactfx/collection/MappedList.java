package org.reactfx.collection;

import java.util.List;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import org.reactfx.Subscription;
import org.reactfx.util.Lists;
import org.reactfx.value.Val;

class MappedList<E, F> extends LiveListBase<F>
implements ReadOnlyLiveListImpl<F> {
    private final ObservableList<? extends E> source;
    private final Function<? super E, ? extends F> mapper;

    public MappedList(
            ObservableList<? extends E> source,
            Function<? super E, ? extends F> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public F get(int index) {
        return mapper.apply(source.get(index));
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    protected Subscription observeInputs() {
        return LiveList.<E>observeQuasiChanges(source, this::sourceChanged);
    }

    private void sourceChanged(QuasiListChange<? extends E> change) {
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
implements ReadOnlyLiveListImpl<F> {
    private final ObservableList<? extends E> source;
    private final ObservableValue<? extends Function<? super E, ? extends F>> mapper;

    public DynamicallyMappedList(
            ObservableList<? extends E> source,
            ObservableValue<? extends Function<? super E, ? extends F>> mapper) {
        this.source = source;
        this.mapper = mapper;
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
                Val.observe(mapper, this::mapperChanged));
    }

    private void sourceChanged(QuasiListChange<? extends E> change) {
        notifyObservers(MappedList.mappedChangeView(change, mapper.getValue()));
    }

    private void mapperChanged(Function<? super E, ? extends F> oldMapper) {
        fireContentReplacement(Lists.mappedView(source, oldMapper));
    }
}