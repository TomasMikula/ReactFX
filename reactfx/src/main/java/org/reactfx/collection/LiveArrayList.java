package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.reactfx.Subscription;

public final class LiveArrayList<E> extends LiveListBase<E> {
    private List<E> list;

    public LiveArrayList() {
        list = new ArrayList<>();
    }

    public LiveArrayList(Collection<? extends E> c) {
        list = new ArrayList<>(c);
    }

    @SafeVarargs
    public LiveArrayList(E... initialElements) {
        this(Arrays.asList(initialElements));
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E element) {
        E replaced = list.set(index, element);
        fireElemReplacement(index, replaced);
        return replaced;
    }

    @Override
    public boolean setAll(Collection<? extends E> c) {
        List<E> removed = list;
        list = new ArrayList<>(c);
        fireContentReplacement(removed);
        return true;
    }

    @SafeVarargs
    @Override
    public final boolean setAll(E... elems) {
        return setAll(Arrays.asList(elems));
    }

    @Override
    public void add(int index, E element) {
        list.add(index, element);
        fireElemInsertion(index);
    }

    @Override
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        list.addAll(index, c);
        fireRangeInsertion(index, c.size());
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size(), c);
    }

    @SafeVarargs
    @Override
    public final boolean addAll(E... elems) {
        return addAll(Arrays.asList(elems));
    }

    @Override
    public E remove(int index) {
        E removed = list.remove(index);
        fireElemRemoval(index, removed);
        return removed;
    }

    @Override
    public void remove(int from, int to) {
        List<E> sublist = list.subList(from, to);
        List<E> removed = new ArrayList<>(sublist);
        sublist.clear();
        fireRemoveRange(from, removed);
    }

    @Override
    public boolean remove(Object o) {
        int i = list.indexOf(o);
        if(i != -1) {
            remove(i);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        ListChangeAccumulator<E> acc = new ListChangeAccumulator<E>();
        for(Object o: c) {
            int i = list.indexOf(o);
            if(i != -1) {
                E removed = list.remove(i);
                acc.add(ProperLiveList.elemRemoval(i, removed));
            }
        }
        if(acc.isEmpty()) {
            return false;
        } else {
            notifyObservers(acc.fetch());
            return true;
        }
    }

    @SafeVarargs
    @Override
    public final boolean removeAll(E... elems) {
        return removeAll(Arrays.asList(elems));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        ListChangeAccumulator<E> acc = new ListChangeAccumulator<E>();
        for(int i = size() - 1; i >= 0; --i) {
            E elem = list.get(i);
            if(!c.contains(elem)) {
                list.remove(i);
                acc.add(ProperLiveList.elemRemoval(i, elem));
            }
        }
        if(acc.isEmpty()) {
            return false;
        } else {
            notifyObservers(acc.fetch());
            return true;
        }
    }

    @SafeVarargs
    @Override
    public final boolean retainAll(E... elems) {
        return retainAll(Arrays.asList(elems));
    }

    @Override
    public void clear() {
        setAll(Collections.emptyList());
    }

    @Override
    protected Subscription observeInputs() {
        return Subscription.EMPTY;
    }
}
