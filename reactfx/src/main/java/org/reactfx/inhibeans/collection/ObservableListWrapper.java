package org.reactfx.inhibeans.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;

import org.reactfx.Guard;
import org.reactfx.ListHelper;

class ObservableListWrapper<E> implements ObservableList<E> {
    private final javafx.collections.ObservableList<E> delegate;

    private ListHelper<InvalidationListener> invalidationListeners = null;
    private ListHelper<ListChangeListener<? super E>> listListeners = null;

    private boolean blocked;
    private final List<SingleListChange<E>> pendingChanges = new ArrayList<>();

    ObservableListWrapper(javafx.collections.ObservableList<E> delegate) {
        this.delegate = delegate;
        delegate.addListener((Change<? extends E> change) -> {
            if(blocked) {
                incorporateChange(change);
            } else {
                notifyListeners(change);
            }
        });
    }

    @Override
    public Guard block() {
        if(blocked) {
            return Guard.EMPTY_GUARD;
        } else {
            blocked = true;
            return this::release;
        }
    }

    private void release() {
        blocked = false;
        if(!pendingChanges.isEmpty()) {
            Change<E> change = squash(pendingChanges);
            pendingChanges.clear();
            notifyListeners(change);
        }
    }

    private Change<E> squash(List<SingleListChange<E>> changeList) {
        return new Change<E>(this) {
            @SuppressWarnings("unchecked")
            private final SingleListChange<E>[] changes =
                (SingleListChange<E>[]) changeList.toArray(new SingleListChange<?>[changeList.size()]);

            private int current = -1;

            @Override
            public int getFrom() {
                return changes[current].getFrom();
            }

            @Override
            protected int[] getPermutation() {
                throw new AssertionError("Unreachable code");
            }

            @Override
            public boolean wasPermutated() {
                return changes[current].isPermutation();
            }

            @Override
            public int getPermutation(int i) {
                return changes[current].getPermutation(i);
            }

            @Override
            public List<E> getRemoved() {
                return changes[current].getRemoved();
            }

            @Override
            public int getTo() {
                return changes[current].getTo();
            }

            @Override
            public boolean next() {
                if(current + 1 < changes.length) {
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
        };
    }

    private void incorporateChange(Change<? extends E> change) {
        while(change.next()) {
            int from = change.getFrom();
            if(change.wasPermutated()) {
                int len = change.getTo() - from;
                int[] permutation = new int[len];
                List<E> replaced = new ArrayList<>(len);
                for(int i = 0; i < len; ++i) {
                    int pi = change.getPermutation(from + i);
                    permutation[i] = pi - from;
                    replaced.add(delegate.get(pi));
                }
                incorporateChange(new Permutation<>(from, permutation, replaced));
            } else {
                @SuppressWarnings("unchecked") // cast is safe because the list is unmodifiable
                List<E> removed = (List<E>) change.getRemoved();
                incorporateChange(new Replacement<>(from, removed, change.getAddedSize()));
            }
        }
    }

    private void incorporateChange(SingleListChange<E> change) {
        if(pendingChanges.isEmpty()) {
            pendingChanges.add(change);
        } else {
            // find first and last overlapping change
            int from = change.getFrom();
            int to = from + change.getOldLength();
            int firstOverlapping = 0;
            for(; firstOverlapping < pendingChanges.size(); ++firstOverlapping) {
                if(pendingChanges.get(firstOverlapping).getTo() >= from) {
                    break;
                }
            }
            int lastOverlapping = pendingChanges.size() - 1;
            for(; lastOverlapping >= 0; --lastOverlapping) {
                if(pendingChanges.get(lastOverlapping).getFrom() <= to) {
                    break;
                }
            }

            // offset changes farther in the list
            int diff = change.getTo() - change.getFrom() - change.getOldLength();
            offsetPendingChanges(lastOverlapping + 1, diff);

            // combine overlapping changes into one
            if(lastOverlapping < firstOverlapping) { // no overlap
                pendingChanges.add(firstOverlapping, change);
            } else { // overlaps one or more former changes
                List<SingleListChange<E>> overlapping = pendingChanges.subList(firstOverlapping, lastOverlapping + 1);
                SingleListChange<E> joined = join(overlapping, change.getReplaced(), change.getFrom());
                SingleListChange<E> newChange = combine(joined, change);
                overlapping.clear();
                pendingChanges.add(firstOverlapping, newChange);
            }
        }
    }

    private void offsetPendingChanges(int from, int offset) {
        pendingChanges.subList(from, pendingChanges.size())
                .replaceAll(change -> change.offset(offset));
    }

    private SingleListChange<E> join(List<SingleListChange<E>> changes, List<E> gone, int goneOffset) {
        if(changes.size() == 1) {
            return changes.get(0);
        }

        List<E> removed = new ArrayList<>();
        SingleListChange<E> prev = changes.get(0);
        int from = prev.getFrom();
        removed.addAll(prev.getReplaced());
        for(int i = 1; i < changes.size(); ++i) {
            SingleListChange<E> ch = changes.get(i);
            removed.addAll(gone.subList(prev.getTo() - goneOffset, ch.getFrom() - goneOffset));
            removed.addAll(ch.getReplaced());
            prev = ch;
        }
        return new Replacement<>(from, removed, prev.getTo() - from);
    }

    private SingleListChange<E> combine(
            SingleListChange<E> former,
            SingleListChange<E> latter) {

        if(latter.getFrom() >= former.getFrom() && latter.getFrom() + latter.getOldLength() <= former.getTo()) {
            // latter is within former
            List<E> removed = former.getReplaced();
            int addedSize = former.getNewLength() - latter.getOldLength() + latter.getNewLength();
            return new Replacement<>(former.getFrom(), removed, addedSize);
        } else if(latter.getFrom() <= former.getFrom() && latter.getFrom() + latter.getOldLength() >= former.getTo()) {
            // former is within latter
            List<E> removed = concat(
                    latter.getReplaced().subList(0, former.getFrom() - latter.getFrom()),
                    former.getReplaced(),
                    latter.getReplaced().subList(former.getTo() - latter.getFrom(), latter.getOldLength()));
            int addedSize = latter.getNewLength();
            return new Replacement<>(latter.getFrom(), removed, addedSize);
        } else if(latter.getFrom() >= former.getFrom()) {
            // latter overlaps to the right
            List<E> removed = concat(
                    former.getReplaced(),
                    latter.getReplaced().subList(former.getTo() - latter.getFrom(), latter.getOldLength()));
            int addedSize = latter.getFrom() - former.getFrom() + latter.getNewLength();
            return new Replacement<>(former.getFrom(), removed, addedSize);
        } else {
            // latter overlaps to the left
            List<E> removed = concat(
                    latter.getReplaced().subList(0, former.getFrom() - latter.getFrom()),
                    former.getReplaced());
            int addedSize = former.getTo() - (latter.getFrom() + latter.getOldLength()) + latter.getNewLength();
            return new Replacement<>(latter.getFrom(), removed, addedSize);
        }
    }

    @SafeVarargs
    private static <T> List<T> concat(List<T>... lists) {
        int n = Arrays.asList(lists).stream().mapToInt(List::size).sum();
        List<T> res = new ArrayList<>(n);
        for(List<T> l: lists) {
            res.addAll(l);
        }
        return res;
    }

    private void notifyListeners(Change<? extends E> change) {
        ListHelper.forEach(invalidationListeners, l -> l.invalidated(this));
        ListHelper.forEach(listListeners, l -> l.onChanged(change));
    }

    @Override
    public void addListener(ListChangeListener<? super E> listener) {
        listListeners = ListHelper.add(listListeners, listener);
    }

    @Override
    public void removeListener(ListChangeListener<? super E> listener) {
        listListeners = ListHelper.remove(listListeners, listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        invalidationListeners = ListHelper.add(invalidationListeners, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners = ListHelper.remove(invalidationListeners, listener);
    }

    @SafeVarargs
    @Override
    public final boolean addAll(E... elements) {
        return delegate.addAll(elements);
    }

    @Override
    public void remove(int from, int to) {
        delegate.remove(from, to);
    }

    @SafeVarargs
    @Override
    public final boolean removeAll(E... elements) {
        return delegate.removeAll(elements);
    }

    @SafeVarargs
    @Override
    public final boolean retainAll(E... elements) {
        return delegate.retainAll(elements);
    }

    @SafeVarargs
    @Override
    public final boolean setAll(E... elements) {
        return delegate.setAll(elements);
    }

    @Override
    public boolean setAll(Collection<? extends E> c) {
        return delegate.setAll(c);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return delegate.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

    @Override
    public E set(int index, E element) {
        return delegate.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        delegate.add(index, element);
    }

    @Override
    public E remove(int index) {
        return delegate.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return delegate.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }
}
