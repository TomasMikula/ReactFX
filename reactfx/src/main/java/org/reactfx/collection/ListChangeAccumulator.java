package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener;

public final class ListChangeAccumulator<E> implements ListModificationSequence<E> {
    private ListChangeImpl<E> modifications = new ListChangeImpl<>();

    public ListChangeAccumulator() {}

    public ListChangeAccumulator(ListChange<E> change) {
        modifications = new ListChangeImpl<>(change);
    }

    @Override
    public ListChangeAccumulator<E> asListChangeAccumulator() {
        return this;
    }

    @Override
    public ListChange<E> asListChange() {
        return fetch();
    }

    @Override
    public List<TransientListModification<E>> getModifications() {
        return Collections.unmodifiableList(modifications);
    }

    public boolean isEmpty() {
        return modifications.isEmpty();
    }

    public ListChange<E> fetch() {
        ListChange<E> res = modifications;
        modifications = new ListChangeImpl<>();
        return res;
    }

    public ListChangeAccumulator<E> drop(int n) {
        modifications.subList(0, n).clear();
        return this;
    }

    public ListChangeAccumulator<E> add(TransientListModification<? extends E> change) {
        if(modifications.isEmpty()) {
            modifications.add(TransientListModification.safeCast(change));
        } else {
            // find first and last overlapping change
            int from = change.getFrom();
            int to = from + change.getRemovedSize();
            int firstOverlapping = 0;
            for(; firstOverlapping < modifications.size(); ++firstOverlapping) {
                if(modifications.get(firstOverlapping).getTo() >= from) {
                    break;
                }
            }
            int lastOverlapping = modifications.size() - 1;
            for(; lastOverlapping >= 0; --lastOverlapping) {
                if(modifications.get(lastOverlapping).getFrom() <= to) {
                    break;
                }
            }

            // offset changes farther in the list
            int diff = change.getTo() - change.getFrom() - change.getRemovedSize();
            offsetPendingChanges(lastOverlapping + 1, diff);

            // combine overlapping changes into one
            if(lastOverlapping < firstOverlapping) { // no overlap
                modifications.add(firstOverlapping, TransientListModification.safeCast(change));
            } else { // overlaps one or more former changes
                List<TransientListModification<E>> overlapping = modifications.subList(firstOverlapping, lastOverlapping + 1);
                TransientListModification<? extends E> joined = join(overlapping, change.getRemoved(), change.getFrom());
                TransientListModification<E> newChange = combine(joined, change);
                overlapping.clear();
                modifications.add(firstOverlapping, newChange);
            }
        }

        return this;
    }

    public ListChangeAccumulator<E> add(ListChange<? extends E> change) {
        for(TransientListModification<? extends E> mod: change) {
            add(TransientListModification.safeCast(mod));
        }

        return this;
    }

    public ListChangeAccumulator<E> add(ListChangeListener.Change<? extends E> change) {
        while(change.next()) {
            add(TransientListModification.fromCurrentStateOf(change));
        }

        return this;
    }

    private void offsetPendingChanges(int from, int offset) {
        modifications.subList(from, modifications.size())
                .replaceAll(change -> new TransientListModificationImpl<>(
                        change.getList(),
                        change.getFrom() + offset,
                        change.getTo() + offset,
                        change.getRemoved()));
    }

    private static <E> TransientListModification<? extends E> join(
            List<TransientListModification<E>> changes,
            List<? extends E> gone,
            int goneOffset) {

        if(changes.size() == 1) {
            return changes.get(0);
        }

        List<E> removed = new ArrayList<>();
        TransientListModification<? extends E> prev = changes.get(0);
        int from = prev.getFrom();
        removed.addAll(prev.getRemoved());
        for(int i = 1; i < changes.size(); ++i) {
            TransientListModification<? extends E> ch = changes.get(i);
            removed.addAll(gone.subList(prev.getTo() - goneOffset, ch.getFrom() - goneOffset));
            removed.addAll(ch.getRemoved());
            prev = ch;
        }
        return new TransientListModificationImpl<>(prev.getList(), from, prev.getTo(), removed);
    }

    private static <E> TransientListModification<E> combine(
            TransientListModification<? extends E> former,
            TransientListModification<? extends E> latter) {

        if(latter.getFrom() >= former.getFrom() && latter.getFrom() + latter.getRemovedSize() <= former.getTo()) {
            // latter is within former
            List<? extends E> removed = former.getRemoved();
            int to = former.getTo() - latter.getRemovedSize() + latter.getAddedSize();
            return new TransientListModificationImpl<>(former.getList(), former.getFrom(), to, removed);
        } else if(latter.getFrom() <= former.getFrom() && latter.getFrom() + latter.getRemovedSize() >= former.getTo()) {
            // former is within latter
            List<E> removed = concat(
                    latter.getRemoved().subList(0, former.getFrom() - latter.getFrom()),
                    former.getRemoved(),
                    latter.getRemoved().subList(former.getTo() - latter.getFrom(), latter.getRemovedSize()));
            return new TransientListModificationImpl<>(latter.getList(), latter.getFrom(), latter.getTo(), removed);
        } else if(latter.getFrom() >= former.getFrom()) {
            // latter overlaps to the right
            List<E> removed = concat(
                    former.getRemoved(),
                    latter.getRemoved().subList(former.getTo() - latter.getFrom(), latter.getRemovedSize()));
            return new TransientListModificationImpl<>(former.getList(), former.getFrom(), latter.getTo(), removed);
        } else {
            // latter overlaps to the left
            List<E> removed = concat(
                    latter.getRemoved().subList(0, former.getFrom() - latter.getFrom()),
                    former.getRemoved());
            int to = former.getTo() - latter.getRemovedSize() + latter.getAddedSize();
            return new TransientListModificationImpl<>(latter.getList(), latter.getFrom(), to, removed);
        }
    }

    @SafeVarargs
    private static <T> List<T> concat(List<? extends T>... lists) {
        int n = Arrays.asList(lists).stream().mapToInt(List::size).sum();
        List<T> res = new ArrayList<>(n);
        for(List<? extends T> l: lists) {
            res.addAll(l);
        }
        return res;
    }
}