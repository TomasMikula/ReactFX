package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener;

import org.reactfx.util.Lists;

/**
 * Accumulates {@link QuasiListChange}s until {@link #fetch()} is called, which returns the accumulated list
 * and then clears its list of accumulations. During the accumulation process, modifications can be added or
 * {@link #drop(int) dropped} before {@code fetch()} is called.
 */
public final class ListChangeAccumulator<E> implements ListModificationSequence<E> {
    private QuasiListChangeImpl<E> modifications = new QuasiListChangeImpl<>();

    public ListChangeAccumulator() {}

    public ListChangeAccumulator(QuasiListChange<E> change) {
        modifications = new QuasiListChangeImpl<>(change);
    }

    @Override
    public ListChangeAccumulator<E> asListChangeAccumulator() {
        return this;
    }

    @Override
    public QuasiListChange<E> asListChange() {
        return fetch();
    }

    @Override
    public List<QuasiListModification<? extends E>> getModifications() {
        return Collections.unmodifiableList(modifications);
    }

    public boolean isEmpty() {
        return modifications.isEmpty();
    }

    /**
     * Returns the current list of accumulated {@link QuasiListModification}s and then sets its
     * list of accumulated changes to an empty list.
     */
    public QuasiListChange<E> fetch() {
        QuasiListChange<E> res = modifications;
        modifications = new QuasiListChangeImpl<>();
        return res;
    }

    /**
     * Clears out the list of accumulated {@link QuasiListModification}s from index 0 to index {@code n}. In other
     * words {@code modifications.subList(0, n).clear()}.
     */
    public ListChangeAccumulator<E> drop(int n) {
        modifications.subList(0, n).clear();
        return this;
    }

    /**
     * Adds the {@link QuasiListModification} to the list of accumulated modifications and combines overlapping
     * modifications into one.
     */
    public ListChangeAccumulator<E> add(QuasiListModification<? extends E> mod) {
        if(modifications.isEmpty()) {
            modifications.add(mod);
        } else {
            // find first and last overlapping modification
            int from = mod.getFrom();
            int to = from + mod.getRemovedSize();
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

            // offset modifications farther in the list
            int diff = mod.getTo() - mod.getFrom() - mod.getRemovedSize();
            offsetPendingModifications(lastOverlapping + 1, diff);

            // combine overlapping modifications into one
            if(lastOverlapping < firstOverlapping) { // no overlap
                modifications.add(firstOverlapping, mod);
            } else { // overlaps one or more former modifications
                List<QuasiListModification<? extends E>> overlapping = modifications.subList(firstOverlapping, lastOverlapping + 1);
                QuasiListModification<? extends E> joined = join(overlapping, mod.getRemoved(), mod.getFrom());
                QuasiListModification<E> newMod = combine(joined, mod);
                overlapping.clear();
                modifications.add(firstOverlapping, newMod);
            }
        }

        return this;
    }

    public ListChangeAccumulator<E> add(QuasiListChange<? extends E> change) {
        for(QuasiListModification<? extends E> mod: change) {
            add(mod);
        }

        return this;
    }

    public ListChangeAccumulator<E> add(ListChangeListener.Change<? extends E> change) {
        while(change.next()) {
            add(QuasiListModification.fromCurrentStateOf(change));
        }

        return this;
    }

    private void offsetPendingModifications(int from, int offset) {
        modifications.subList(from, modifications.size())
                .replaceAll(mod -> new QuasiListModificationImpl<>(
                        mod.getFrom() + offset,
                        mod.getRemoved(),
                        mod.getAddedSize()));
    }

    private static <E> QuasiListModification<? extends E> join(
            List<QuasiListModification<? extends E>> mods,
            List<? extends E> gone,
            int goneOffset) {

        if(mods.size() == 1) {
            return mods.get(0);
        }

        List<List<? extends E>> removedLists = new ArrayList<>(2*mods.size() - 1);
        QuasiListModification<? extends E> prev = mods.get(0);
        int from = prev.getFrom();
        removedLists.add(prev.getRemoved());
        for(int i = 1; i < mods.size(); ++i) {
            QuasiListModification<? extends E> m = mods.get(i);
            removedLists.add(gone.subList(prev.getTo() - goneOffset, m.getFrom() - goneOffset));
            removedLists.add(m.getRemoved());
            prev = m;
        }
        List<E> removed = Lists.concat(removedLists);
        return new QuasiListModificationImpl<>(from, removed, prev.getTo() - from);
    }

    private static <E> QuasiListModification<E> combine(
            QuasiListModification<? extends E> former,
            QuasiListModification<? extends E> latter) {

        if(latter.getFrom() >= former.getFrom() && latter.getFrom() + latter.getRemovedSize() <= former.getTo()) {
            // latter is within former
            List<? extends E> removed = former.getRemoved();
            int addedSize = former.getAddedSize() - latter.getRemovedSize() + latter.getAddedSize();
            return new QuasiListModificationImpl<>(former.getFrom(), removed, addedSize);
        } else if(latter.getFrom() <= former.getFrom() && latter.getFrom() + latter.getRemovedSize() >= former.getTo()) {
            // former is within latter
            List<E> removed = Lists.concat(
                    latter.getRemoved().subList(0, former.getFrom() - latter.getFrom()),
                    former.getRemoved(),
                    latter.getRemoved().subList(former.getTo() - latter.getFrom(), latter.getRemovedSize()));
            return new QuasiListModificationImpl<>(latter.getFrom(), removed, latter.getAddedSize());
        } else if(latter.getFrom() >= former.getFrom()) {
            // latter overlaps to the right
            List<E> removed = Lists.concat(
                    former.getRemoved(),
                    latter.getRemoved().subList(former.getTo() - latter.getFrom(), latter.getRemovedSize()));
            return new QuasiListModificationImpl<>(former.getFrom(), removed, latter.getTo() - former.getFrom());
        } else {
            // latter overlaps to the left
            List<E> removed = Lists.concat(
                    latter.getRemoved().subList(0, former.getFrom() - latter.getFrom()),
                    former.getRemoved());
            int addedSize = former.getTo() - latter.getRemovedSize() + latter.getAddedSize() - latter.getFrom();
            return new QuasiListModificationImpl<>(latter.getFrom(), removed, addedSize);
        }
    }
}