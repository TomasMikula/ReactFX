package org.reactfx.collection;

import java.util.List;

import org.reactfx.util.Lists;

public interface MaterializedListModification<E> extends ListModificationLike<E> {

    /**
     * Doesn't create defensive copies of the passed lists.
     * Therefore, they must not be modified later.
     */
    static <E> MaterializedListModification<E> create(int pos, List<? extends E> removed, List<? extends E> added) {
        return new MaterializedListModificationImpl<E>(pos, removed, added);
    }

    List<? extends E> getAdded();

    @Override
    default int getAddedSize() { return getAdded().size(); }

    default MaterializedListModification<E> trim() {
        return Lists.commonPrefixSuffixLengths(getRemoved(), getAdded()).map((pref, suff) -> {
            if(pref == 0 && suff == 0) {
                return this;
            } else {
                return create(
                        getFrom() + pref,
                        getRemoved().subList(pref, getRemovedSize() - suff),
                        getAdded().subList(pref, getAddedSize() - suff));
            }
        });
    }
}

final class MaterializedListModificationImpl<E>
implements MaterializedListModification<E> {
    private final int from;
    private final List<? extends E> removed;
    private final List<? extends E> added;

    MaterializedListModificationImpl(
            int from, List<? extends E> removed, List<? extends E> added) {
        this.from = from;
        this.removed = removed;
        this.added = added;
    }

    @Override public int getFrom() { return from; }
    @Override public List<? extends E> getRemoved() { return removed; }
    @Override public List<? extends E> getAdded() { return added; }
}