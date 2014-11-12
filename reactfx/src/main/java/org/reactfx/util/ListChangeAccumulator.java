package org.reactfx.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ListChangeAccumulator<E> {
    private List<TransientListChange<? extends E>> changes = new ArrayList<>();

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public List<TransientListChange<? extends E>> fetch() {
        List<TransientListChange<? extends E>> res = changes;
        changes = new ArrayList<>();
        return res;
    }

    public void add(TransientListChange<? extends E> change) {
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
                List<TransientListChange<? extends E>> overlapping = changes.subList(firstOverlapping, lastOverlapping + 1);
                TransientListChange<? extends E> joined = join(overlapping, change.getRemoved(), change.getFrom());
                TransientListChange<E> newChange = combine(joined, change);
                overlapping.clear();
                changes.add(firstOverlapping, newChange);
            }
        }
    }

    private void offsetPendingChanges(int from, int offset) {
        changes.subList(from, changes.size())
                .replaceAll(change -> new TransientListChangeImpl<>(
                        change.getList(),
                        change.getFrom() + offset,
                        change.getTo() + offset,
                        change.getRemoved()));
    }

    private static <E> TransientListChange<? extends E> join(
            List<TransientListChange<? extends E>> changes,
            List<? extends E> gone,
            int goneOffset) {

        if(changes.size() == 1) {
            return changes.get(0);
        }

        List<E> removed = new ArrayList<>();
        TransientListChange<? extends E> prev = changes.get(0);
        int from = prev.getFrom();
        removed.addAll(prev.getRemoved());
        for(int i = 1; i < changes.size(); ++i) {
            TransientListChange<? extends E> ch = changes.get(i);
            removed.addAll(gone.subList(prev.getTo() - goneOffset, ch.getFrom() - goneOffset));
            removed.addAll(ch.getRemoved());
            prev = ch;
        }
        return new TransientListChangeImpl<>(prev.getList(), from, prev.getTo(), removed);
    }

    private static <E> TransientListChange<E> combine(
            TransientListChange<? extends E> former,
            TransientListChange<? extends E> latter) {

        if(latter.getFrom() >= former.getFrom() && latter.getFrom() + latter.getRemovedSize() <= former.getTo()) {
            // latter is within former
            List<? extends E> removed = former.getRemoved();
            int to = former.getTo() - latter.getRemovedSize() + latter.getAddedSize();
            return new TransientListChangeImpl<>(former.getList(), former.getFrom(), to, removed);
        } else if(latter.getFrom() <= former.getFrom() && latter.getFrom() + latter.getRemovedSize() >= former.getTo()) {
            // former is within latter
            List<E> removed = concat(
                    latter.getRemoved().subList(0, former.getFrom() - latter.getFrom()),
                    former.getRemoved(),
                    latter.getRemoved().subList(former.getTo() - latter.getFrom(), latter.getRemovedSize()));
            return new TransientListChangeImpl<>(latter.getList(), latter.getFrom(), latter.getTo(), removed);
        } else if(latter.getFrom() >= former.getFrom()) {
            // latter overlaps to the right
            List<E> removed = concat(
                    former.getRemoved(),
                    latter.getRemoved().subList(former.getTo() - latter.getFrom(), latter.getRemovedSize()));
            return new TransientListChangeImpl<>(former.getList(), former.getFrom(), latter.getTo(), removed);
        } else {
            // latter overlaps to the left
            List<E> removed = concat(
                    latter.getRemoved().subList(0, former.getFrom() - latter.getFrom()),
                    former.getRemoved());
            int to = former.getTo() - latter.getRemovedSize() + latter.getAddedSize();
            return new TransientListChangeImpl<>(latter.getList(), latter.getFrom(), to, removed);
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