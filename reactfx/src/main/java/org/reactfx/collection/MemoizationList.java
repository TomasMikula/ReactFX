package org.reactfx.collection;

import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;

import org.reactfx.Subscription;
import org.reactfx.util.Lists;
import org.reactfx.util.SparseList;

/**
 * A list for handling which items from its {@code sourceList} appear in its {@link #memoizedItems() memoized list}.
 * When {@link #isMemoized(int) this list is memoized}, {@link #memoizedItems()} will only contain some of the items
 * from the {@code sourceList}. Otherwise, it will contain all of those items. <b>Note: {@link #memoizedItems()}'
 * items do not need to be continuous. It can have the {@code sourceList}'s first and last item while ignoring the
 * middle items.</b>
 *
 * <p>
 *     The methods, {@link #force(int, int)} and {@link #forget(int, int)}, will throw {@link IllegalStateException}
 *     if they are called when this list or its {@link #memoizedItems()} list is not currently observing its inputs.
 *     One can force the observation of its inputs by calling {@link LiveList#pin()} or any other
 *     "{@code observe-}" related methods on this list or its {@link #memoizedItems()} list.
 * </p>

 */
public interface MemoizationList<E> extends LiveList<E> {

    /**
     * Returns a unmodifiable list that either contains all of this list's {@code sourceList} when not memoized or
     * some of that list's items when memoized.
     */
    LiveList<E> memoizedItems();

    /**
     * True if the item at the given index in the {@code sourceList} is also in the {@link #memoizedItems()} list.
     */
    boolean isMemoized(int index);

    /**
     * Gets the item at {@code index} if it is listed in the {@link #memoizedItems()}. Otherwise, returns
     * {@link Optional#empty()}
     */
    Optional<E> getIfMemoized(int index);

    /**
     * Gets the size of {@link #memoizedItems()}
     */
    int getMemoizedCount();

    /**
     * Gets the number of items in {@link #memoizedItems()} that occur before the given position in {@link #memoizedItems()}.
     */
    int getMemoizedCountBefore(int position);

    /**
     * Gets the number of items in {@link #memoizedItems()} that occur after the given position in {@link #memoizedItems()}.
     */
    int getMemoizedCountAfter(int position);

    /**
     * Excludes the items in the specified range (using the {@code sourceList}'s index system) from
     * {@link #memoizedItems()}
     */
    void forget(int from, int to);

    /**
     * Using the {@code index} of an item in {@link #memoizedItems()}, returns the index of that same item in this
     * list's {@code sourceList}.
     */
    int indexOfMemoizedItem(int index);

    /**
     * Gets the lower and upper index bounds (according to the {@code sourceList}'s indexes) of the first and last
     * item in {@link #memoizedItems()}. <b>Note: remember that the memoized items may not be continuous</b>
     */
    IndexRange getMemoizedItemsRange();

    /**
     * Forces the {@link #memoizedItems()} to only include the items in the specified range (according to the
     * {@code sourceList}'s index system.
     */
    void force(int from, int to);

}

class MemoizationListImpl<E>
extends LiveListBase<E>
implements MemoizationList<E>, UnmodifiableByDefaultLiveList<E> {

    private class MemoizedView
    extends LiveListBase<E>
    implements UnmodifiableByDefaultLiveList<E> {

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

        private void prepareNotifications(QuasiListChange<? extends E> change) {
            enqueueNotifications(change);
        }

        private void prepareNotifications(QuasiListModification<? extends E> mod) {
            enqueueNotifications(mod.asListChange());
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
        return LiveList.<E>observeQuasiChanges(source, this::sourceChanged)
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
        if(!isObservingInputs()) { // memoization is off
            return source.get(index);
        } else if(sparseList.isPresent(index)) {
            return sparseList.getOrThrow(index);
        } else {
            E elem = source.get(index); // may cause recursive get(), so we
                                        // need to check again for absence
            if(sparseList.setIfAbsent(index, elem)) {
                memoizedItems.fireElemInsertion(
                        sparseList.getPresentCountBefore(index));
            }
            return elem;
        }
    }

    @Override
    public void force(int from, int to) {
        if(!isObservingInputs()) { // memoization is off
            throw new IllegalStateException(
                    "Cannot force items when memoization is off."
                    + " To turn memoization on, you have to be observing this"
                    + " list or its memoizedItems.");
        }

        Lists.checkRange(from, to, size());
        for(int i = from; i < to; ++i) {
            if(!sparseList.isPresent(i)) {
                E elem = source.get(i);
                if(sparseList.setIfAbsent(i, elem)) {
                    int presentBefore = sparseList.getPresentCountBefore(i);
                    memoizedItems.prepareNotifications(ProperLiveList.elemInsertion(presentBefore));
                }
            }
        }
        memoizedItems.publishNotifications();
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    public LiveList<E> memoizedItems() {
        return memoizedItems;
    }

    @Override
    public boolean isMemoized(int index) {
        return isObservingInputs() && sparseList.isPresent(index);
    }

    @Override
    public Optional<E> getIfMemoized(int index) {
        Lists.checkIndex(index, size());
        return isObservingInputs()
                ? sparseList.get(index)
                : Optional.empty();
    }

    @Override
    public int getMemoizedCountBefore(int position) {
        Lists.checkPosition(position, size());
        return isObservingInputs()
                ? sparseList.getPresentCountBefore(position)
                : 0;
    }

    @Override
    public int getMemoizedCountAfter(int position) {
        Lists.checkPosition(position, size());
        return isObservingInputs()
                ? sparseList.getPresentCountAfter(position)
                : 0;
    }

    @Override
    public int getMemoizedCount() {
        return memoizedItems.size();
    }

    @Override
    public void forget(int from, int to) {
        if(!isObservingInputs()) { // memoization is off
            throw new IllegalStateException(
                    "There is nothing to forget, because memoization is off."
                    + " To turn memoization on, you have to be observing this"
                    + " list or its memoizedItems.");
        }

        Lists.checkRange(from, to, size());
        int memoChangeFrom = sparseList.getPresentCountBefore(from);
        List<E> memoRemoved = sparseList.collect(from, to);
        sparseList.spliceByVoid(from, to, to - from);
        memoizedItems.fireRemoveRange(memoChangeFrom, memoRemoved);
    }

    @Override
    public int indexOfMemoizedItem(int index) {
        return sparseList.indexOfPresentItem(index);
    }

    @Override
    public IndexRange getMemoizedItemsRange() {
        return sparseList.getPresentItemsRange();
    }
}