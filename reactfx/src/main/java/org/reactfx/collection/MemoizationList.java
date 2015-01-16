package org.reactfx.collection;

import java.util.List;

import javafx.collections.ObservableList;

import org.reactfx.Subscription;
import org.reactfx.util.SparseList;

public interface MemoizationList<E> extends ObsList<E> {
    ObsList<E> memoizedItems();
}

class MemoizationListImpl<E>
extends ObsListBase<E>
implements MemoizationList<E>, ReadOnlyObsListImpl<E> {

    private class MemoizedView
    extends ObsListBase<E>
    implements ReadOnlyObsListImpl<E> {

        @Override
        protected Subscription observeInputs() {
            return MemoizationListImpl.this.pin();
        }

        @Override
        public E get(int index) {
            return sparseList.getPresent(index);
        }

        @Override
        public int size() {
            return sparseList.getPresentCount();
        }

        private void prepareNotifications(QuasiListChange<? extends E> event) {
            enqueueNotifications(event);
        }

        private void publishNotifications() {
            notifyObservers();
        }
    }

    private final SparseList<E> sparseList = new SparseList<>();
    private final MemoizedView memoizedItems = new MemoizedView();
    private final ObservableList<E> source;

    MemoizationListImpl(ObservableList<E> source) {
        this.source = source;
    }

    @Override
    protected Subscription observeInputs() {
        sparseList.insertVoid(0, source.size());
        return ObsList.<E>observeQuasiChanges(source, this::sourceChanged)
            .and(sparseList::clear);
    }

    private void sourceChanged(QuasiListChange<? extends E> qc) {
        ListChangeAccumulator<E> acc = new ListChangeAccumulator<>();
        for(QuasiListModification<? extends E> mod: qc) {
            int from = mod.getFrom();
            int removedSize = mod.getRemovedSize();
            int memoFrom = sparseList.getPresentCountBefore(from);
            List<E> memoRemoved = sparseList.collect(from, from + removedSize);
            sparseList.spliceByVoid(from, from + removedSize, mod.getAddedSize());
            acc.add(new QuasiListModificationImpl<>(memoFrom, memoRemoved, 0));
        }
        memoizedItems.prepareNotifications(acc.fetch());
        notifyObservers(qc);
        memoizedItems.publishNotifications();
    }

    @Override
    public E get(int index) {
        if(sparseList.isPresent(index)) {
            return sparseList.getOrThrow(index);
        } else {
            E elem = source.get(index);
            sparseList.set(index, elem);
            memoizedItems.fireElemInsertion(
                    sparseList.getPresentCountBefore(index));
            return elem;
        }
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    public ObsList<E> memoizedItems() {
        return memoizedItems;
    }
}