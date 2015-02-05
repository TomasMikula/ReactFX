package org.reactfx.inhibeans.property;

import org.reactfx.inhibeans.value.ObservableValue;
import org.reactfx.value.SuspendableVar;

/**
 * @deprecated Superseded by {@link SuspendableVar}.
 */
@Deprecated
public interface Property<T>
extends javafx.beans.property.Property<T>, ObservableValue<T> {}
