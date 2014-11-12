package org.reactfx.inhibeans.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;

import org.reactfx.Guard;
import org.reactfx.util.ListChangeAccumulator;
import org.reactfx.util.ListHelper;
import org.reactfx.util.TransientListChange;

class ObservableListWrapper<E> implements ObservableList<E> {
    private final javafx.collections.ObservableList<E> delegate;

    private ListHelper<InvalidationListener> invalidationListeners = null;
    private ListHelper<ListChangeListener<? super E>> listListeners = null;

    private boolean blocked;
    private final ListChangeAccumulator<E> pendingChanges = new ListChangeAccumulator<>();

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
            Change<E> change = squash(pendingChanges.fetch());
            notifyListeners(change);
        }
    }

    private Change<E> squash(List<TransientListChange<? extends E>> changeList) {
        return new Change<E>(this) {
            @SuppressWarnings("unchecked")
            private final TransientListChange<? extends E>[] changes =
                (TransientListChange<? extends E>[]) changeList.toArray(new TransientListChange<?>[changeList.size()]);

            private int current = -1;

            @Override
            public int getFrom() {
                return changes[current].getFrom();
            }

            @Override
            protected int[] getPermutation() {
                return new int[0]; // not a permutation
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<E> getRemoved() {
                // cast is safe, because the list is unmodifiable
                return (List<E>) changes[current].getRemoved();
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
            pendingChanges.add(TransientListChange.fromCurrentStateOf(change));
        }
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
