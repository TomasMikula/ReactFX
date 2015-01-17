package org.reactfx.util;

import java.util.Deque;
import java.util.LinkedList;

import org.reactfx.collection.ListChangeAccumulator;
import org.reactfx.collection.ListModificationSequence;
import org.reactfx.collection.QuasiListChange;

public interface AccumulationFacility<T, A> {
    A initialAccumulator(T value);
    A reduce(A accum, T value);

    interface IllegalAccumulation<T, A> extends AccumulationFacility<T, A> {
        @Override
        default A reduce(A accum, T value) { throw new IllegalStateException(); }
    }

    interface HomotypicAccumulation<T> extends AccumulationFacility<T, T> {
        @Override
        default T initialAccumulator(T value) { return value; }
    }

    interface NoAccumulation<T>
    extends IllegalAccumulation<T, T>, HomotypicAccumulation<T> {}

    interface Queuing<T> extends AccumulationFacility<T, Deque<T>> {

        @Override
        default Deque<T> initialAccumulator(T value) {
            Deque<T> res = new LinkedList<>();
            res.add(value);
            return res;
        }

        @Override
        default Deque<T> reduce(Deque<T> accum, T value) {
            accum.addLast(value);
            return accum;
        }
    }

    interface RetainLatest<T> extends HomotypicAccumulation<T> {
        @Override
        default T reduce(T accum, T value) { return value; }
    }

    interface RetainOldest<T> extends HomotypicAccumulation<T> {
        @Override
        default T reduce(T accum, T value) { return accum; }
    }

    interface ListChangeAccumulation<E>
    extends AccumulationFacility<QuasiListChange<? extends E>, ListModificationSequence<E>> {

        @Override
        default ListModificationSequence<E> initialAccumulator(
                QuasiListChange<? extends E> value) {
            return QuasiListChange.safeCast(value);
        }

        @Override
        default ListChangeAccumulator<E> reduce(
                ListModificationSequence<E> accum,
                QuasiListChange<? extends E> value) {
            return accum.asListChangeAccumulator().add(value);
        }
    }
}
