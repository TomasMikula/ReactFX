package org.reactfx.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javafx.collections.ObservableList;

import org.reactfx.SuspendableBase;
import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.NotificationAccumulator;

final class SuspendableListWrapper<E>
extends SuspendableBase<LiveList.Observer<? super E, ?>, QuasiListChange<? extends E>, ListModificationSequence<E>>
implements SuspendableList<E>, ProperLiveList<E> {
    private final ObservableList<E> delegate;

    SuspendableListWrapper(ObservableList<E> source) {
        super(LiveList.quasiChangesOf(source), NotificationAccumulator.listNotifications());
        this.delegate = source;
    }

    @Override
    protected AccumulatorSize sizeOf(ListModificationSequence<E> accum) {
        return AccumulatorSize.ONE;
    }

    @Override
    protected QuasiListChange<? extends E> headOf(ListModificationSequence<E> accum) {
        return accum.asListChange();
    }

    @Override
    protected ListModificationSequence<E> tailOf(
            ListModificationSequence<E> accum) {
        throw new NoSuchElementException();
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
