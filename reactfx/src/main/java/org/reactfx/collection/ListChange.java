package org.reactfx.collection;

/**
 * Stores a list of {@link ListModification}s. Essentially the same as {@link AbstractListModificationSequence} (it
 * just specifies one of its super-interface's generics). It differs from {@link ListModificationSequence})
 * in that no "casting" methods exist between that {@code ListModificationSequence}'s two sub-interfaces.
 */
public interface ListChange<E>
extends AbstractListModificationSequence<E, ListModification<? extends E>> {
}