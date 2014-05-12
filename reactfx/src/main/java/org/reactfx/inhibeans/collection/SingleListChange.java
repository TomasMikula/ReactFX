package org.reactfx.inhibeans.collection;

import java.util.List;

interface SingleListChange<E> {
    int getFrom();
    int getTo();
    boolean isPermutation();
    int getPermutation(int i);
    List<E> getRemoved();
    List<E> getReplaced();
    int getOldLength();
    int getNewLength();
    SingleListChange<E> offset(int offset);
}

class Permutation<E> implements SingleListChange<E> {
    private final int from;
    private final int[] perm;
    private final List<E> replaced;

    public Permutation(int from, int[] perm, List<E> replaced) {
        this.from = from;
        this.perm = perm;
        this.replaced = replaced;
    }
    @Override
    public int getFrom() {
        return from;
    }

    @Override
    public int getTo() {
        return from + perm.length;
    }

    @Override
    public boolean isPermutation() {
        return true;
    }

    @Override
    public int getPermutation(int i) {
        if(i >= from && i < getTo()) {
            return from + perm[i - from];
        } else {
            return i;
        }
    }

    @Override
    public List<E> getRemoved() {
        return java.util.Collections.emptyList();
    }

    @Override
    public List<E> getReplaced() {
        return replaced;
    }

    @Override
    public int getOldLength() {
        return perm.length;
    }

    @Override
    public int getNewLength() {
        return perm.length;
    }

    @Override
    public Permutation<E> offset(int offset) {
        return new Permutation<>(from + offset, perm, replaced);
    }
}

class Replacement<E> implements SingleListChange<E> {
    private final int from;
    private final int addedSize;
    private final List<E> removed;

    public Replacement(int from, List<E> removed, int addedSize) {
        this.from = from;
        this.removed = removed;
        this.addedSize = addedSize;
    }

    @Override
    public int getFrom() {
        return from;
    }

    @Override
    public int getTo() {
        return from + addedSize;
    }

    @Override
    public boolean isPermutation() {
        return false;
    }

    @Override
    public int getPermutation(int i) {
        throw new IllegalStateException("Not a permutation change");
    }

    @Override
    public List<E> getRemoved() {
        return removed;
    }

    @Override
    public List<E> getReplaced() {
        return removed;
    }

    @Override
    public int getOldLength() {
        return removed.size();
    }

    @Override
    public int getNewLength() {
        return addedSize;
    }

    @Override
    public Replacement<E> offset(int offset) {
        return new Replacement<>(from + offset, removed, addedSize);
    }

}