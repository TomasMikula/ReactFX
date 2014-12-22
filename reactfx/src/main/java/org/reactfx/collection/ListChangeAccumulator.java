package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.collections.ListChangeListener;

public final class ListChangeAccumulator<E> {
    private ListChangeImpl<E> changes = new ListChangeImpl<>();

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public ListChange<E> fetch() {
        ListChange<E> res = changes;
        changes = new ListChangeImpl<>();
        return res;
    }

    public void add(TransientListModification<E> change) {
        if(changes.isEmpty()) {
            changes.add(change);
        } else {
            // find first and last overlapping change
            int from = change.getFrom();
            int to = from + change.getRemovedSize();
            int firstOverlapping = 0;
            for(; firstOverlapping < changes.size(); ++firstOverlapping) {
                if(changes.get(firstOverlapping).getTo() >= from) {
                    break;
                }
            }
            int lastOverlapping = changes.size() - 1;
            for(; lastOverlapping >= 0; --lastOverlapping) {
                if(changes.get(lastOverlapping).getFrom() <= to) {
                    break;
                }
            }

            // offset changes farther in the list
            int diff = change.getTo() - change.getFrom() - change.getRemovedSize();
            offsetPendingChanges(lastOverlapping + 1, diff);

            // combine overlapping changes into one
            if(lastOverlapping < firstOverlapping) { // no overlap
                changes.add(firstOverlapping, change);
            } else { // overlaps one or more former changes
                List<TransientListModification<E>> overlapping = changes.subList(firstOverlapping, lastOverlapping + 1);
                TransientListModification<? extends E> joined = join(overlapping, change.getRemoved(), change.getFrom());
                TransientListModification<E> newChange = combine(joined, change);
                overlapping.clear();
                changes.add(firstOverlapping, newChange);
            }
        }
    }

    public void add(ListChange<? extends E> change) {
        for(TransientListModification<? extends E> mod: change.getModifications()) {
            add(TransientListModification.safeCast(mod));
        }
    }

    public void add(ListChangeListener.Change<? extends E> change) {
        while(change.next()) {
            add(TransientListModification.fromCurrentStateOf(change));
        }
    }

    private void offsetPendingChanges(int from, int offset) {
        changes.subList(from, changes.size())
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