package org.reactfx.collection;

import static java.util.Collections.binarySearch;
import static java.util.Collections.emptySortedSet;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.unmodifiableObservableList;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;

import com.google.common.collect.Sets;

/**
 * Implementation of {@link ObservableSortedSet} based on {@link ArrayList}.
 */
public final class ObservableSortedArraySet<E> extends AbstractSet<E> implements ObservableSortedSet<E> {
    private final ObservableList<E> backing = observableArrayList();
    private final Map<E, Observable[]> observables = new IdentityHashMap<>();
    private final Collection<SetChangeListener<? super E>> observers = new CopyOnWriteArrayList<>();
    private final Collection<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();
    private final Comparator<? super E> comparator;
    private final Function<? super E, ? extends Collection<? extends Observable>> resortListenFunction;
    private final InvalidationListener resortListener, resortListenerWeak;
    private final ObservableList<E> listView = unmodifiableObservableList(backing);

    @Override
    public ObservableList<E> listView() {
        return listView;
    }

    /**
     * Constructs a new {@link ObservableSortedArraySet}.
     *
     * <p>The {@code resortListenFunction} parameter takes a function that,
     * given a value stored in the set, yields any number of
     * {@link Observable}s. Whenever any of them are
     * {@linkplain Observable#addListener(InvalidationListener) invalidated},
     * this set is resorted. This way, the sort order of the items in the set
     * (and its {@linkplain #listView list view}) are kept up to date.</p>
     *
     * @param comparator how the items in the set will be compared
     * @param resortListenFunction triggers for re-sorting, as above
     */
    public ObservableSortedArraySet(Comparator<? super E> comparator, Function<? super E, ? extends Collection<? extends Observable>> resortListenFunction) {
        assert backing instanceof RandomAccess
            : "FXCollections.observableArrayList returned an ObservableList that doesn't implement RandomAccess.";

        this.comparator = comparator;
        this.resortListenFunction = resortListenFunction;

        resortListener = obs -> resort();
        resortListenerWeak = new WeakInvalidationListener(resortListener);
    }

    private void resort() {
        backing.sort(comparator);
    }

    private void onAdded(E o) {
        Observable[] os = observables.computeIfAbsent(o, oo -> {
            Collection<? extends Observable> osc = resortListenFunction.apply(oo);
            return osc.toArray(new Observable[osc.size()]);
        });

        for (Observable oo : os) {
            oo.addListener(resortListenerWeak);
        }

        fire(new SetChangeListener.Change<E>(this) {
            @Override
            public boolean wasAdded() {
                return true;
            }

            @Override
            public boolean wasRemoved() {
                return false;
            }

            @Override
            public E getElementAdded() {
                return o;
            }

            @Override
            public E getElementRemoved() {
                return null;
            }
        });
    }

    private void onRemoved(E o) {
        for (Observable oo : observables.remove(o)) {
            oo.removeListener(resortListenerWeak);
        }

        fire(new SetChangeListener.Change<E>(this) {
            @Override
            public boolean wasAdded() {
                return false;
            }

            @Override
            public boolean wasRemoved() {
                return true;
            }

            @Override
            public E getElementAdded() {
                return null;
            }

            @Override
            public E getElementRemoved() {
                return o;
            }
        });
    }

    private void fire(SetChangeListener.Change<E> evt) {
        for (SetChangeListener<? super E> oo : observers) {
            oo.onChanged(evt);
        }

        for (InvalidationListener oo : invalidationListeners) {
            oo.invalidated(this);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> it = backing.iterator();
            private E previous;

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                previous = null;
                it.forEachRemaining(action);
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                previous = it.next();
                return previous;
            }

            @Override
            public void remove() {
                it.remove();
                onRemoved(previous);
            }
        };
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        backing.forEach(action);
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public Stream<E> parallelStream() {
        return backing.parallelStream();
    }

    @Override
    public Stream<E> stream() {
        return backing.stream();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    @Override
    public Object[] toArray() {
        return backing.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backing.toArray(a);
    }

    @Override
    public Spliterator<E> spliterator() {
        return backing.spliterator();
    }

    @Override
    public int size() {
        return backing.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void clear() {
        Object[] os = backing.toArray();
        backing.clear();

        for (Object o : os) {
            onRemoved((E) o);
        }
    }

    @Override
    public void addListener(SetChangeListener<? super E> listener) {
        observers.add(listener);
    }

    @Override
    public void removeListener(SetChangeListener<? super E> listener) {
        observers.remove(listener);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        if (Objects.equals(fromElement, toElement)) {
            return emptySortedSet();
        } else {
            return Sets.filter(this, e -> (comparator.compare(e, fromElement) >= 0) && (comparator.compare(e, toElement) < 0));
        }
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return Sets.filter(this, e -> comparator.compare(e, toElement) < 0);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return Sets.filter(this, e -> comparator.compare(e, fromElement) >= 0);
    }

    @Override
    public E first() {
        if (backing.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return backing.get(0);
        }
    }

    @Override
    public E last() {
        if (backing.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return backing.get(backing.size() - 1);
        }
    }

    @Override
    public boolean add(E e) {
        int pos = binarySearch(backing, e, comparator);

        if (pos >= 0) {
            return false;
        } else {
            backing.add(-pos - 1, e);
            onAdded(e);
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        int pos = binarySearch(backing, (E) o, comparator);
        return (pos >= 0) && backing.get(pos).equals(o);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        int pos = binarySearch(backing, (E) o, comparator);

        if ((pos >= 0) && backing.get(pos).equals(o)) {
            backing.remove(pos);
            onRemoved((E) o);
            return true;
        } else {
            return false;
        }
    }
}