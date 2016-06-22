
package org.reactfx.value;


import java.util.function.Consumer;

import org.reactfx.RigidObservable;

public abstract class RigidVal<T>
extends RigidObservable<Consumer<? super T>> implements Val<T>
{
    
}
