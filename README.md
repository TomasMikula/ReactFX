ReactFX
=======

This project is an exploration of reactive programming techniques for JavaFX. A lot of inspiration is drawn from the [rxJava library](https://github.com/Netflix/RxJava/wiki) and the excellent [Principles of Reactive Programming](https://www.coursera.org/course/reactive) course. Since ReactFX specifically targets JavaFX applications, its design and use can be significantly simpler than that of rxJava. Most importantly, all UI events in JavaFX applications are handled on the JavaFX application thread. Therefore, ReactFX does not need to worry about asynchrony, schedulers, etc.

Event streams
-------------

An `EventStream` emits values (events). You can subscribe to an event stream to get notified each time a value is emitted.

```java
interface EventStream<T> {
    Subscription subscribe(Consumer<T> consumer);
}
```

Example:

```java
EventStream<T> eventStream = ...;
eventStream.subscribe(event -> System.out.println(event));
```

To stop receiving notifications, you use the `Subscription` returned from the `subscribe` method to unsubscribe:

```
Subscription subscription = eventStream.subscribe(event -> System.out.println(event));
// ...
subscription.unsubscribe();
```

Note that you need only the instance of `Subscription` to stop previously requested notifications. Compare this to JavaFX listeners/event handlers, where you need to keep both the listener/handler and the object you are listening to to be able to unregister the listener/handler.


Event streams in JavaFX
-----------------------

Although it has no notion of an event stream, there are many event streams already hiding in JavaFX. ReactFX provides adapter methods to materialize them as `EventStream` instances.


### UI events

Every `Node` is capable of emitting various types of events. We can obtain an event stream for each event type:

```
EventStream<MouseEvent> clicks = EventStreams.eventsOf(node, MouseEvent.MOUSE_CLICKED);
clicks.subscribe(click -> System.out.println("Click!"));
```


### ObservableValue invalidations and changes

Every `ObservableValue` (e.g. property, binding) emits invalidations and changes. We can obtain the respective event streams:

```java
ObservableValue<T> observable = ...;
EventStream<Void> invalidations = EventStreams.invalidationsOf(observable);
EventStream<Change<T>> changes = EventStreams.changesOf(observable);
EventStream<T> values = EventStreams.valuesOf(observable);
```

The `values` stream above emits the new value every time the value changes. As opposed to the `changes` stream above, it avoids creating a `Change` instance in case we're not interested in the old value.


Custom event streams
--------------------

`EventSource` is an event stream that emits precisely what you _push_ into it.

```java
EventSource<Integer> numbers = new EventSource<>();
numbers.subscribe(i -> System.out.println(i));
numbers.push(7); // prints "7"
```


Stream composition
------------------

Fun begins with combining streams into new streams.


### filter

```java
EventStream<MouseEvent> clicks = EventStreams.eventsOf(node, MouseEvent.MOUSE_CLICKED);
EventStream<MouseEvent> leftClicks = clicks.filter(click -> click.getButton() == MouseButton.PRIMARY);
```


### map

```java
EventStream<KeyEvent> keysTyped = EventStreams.eventsOf(node, KeyEvent.KEY_TYPED);
EventStream<String> charsTyped = keysTyped.map(keyEvt -> keyEvt.getCharacter());
```


### merge

```java
EventStream<T> stream1 = ...;
EventStream<T> stream2 = ...;
EventStream<T> merged = EventStreams.merge(stream1, stream2);
```


### combine-by

```java
EventStream<Double> widths = ...;
EventStream<Double> heights = ...;
EventStream<Double> areas = EventStreams.combine(widths, heights).by((w, h) -> w * h);
```

`areas` emits a combined value every time _either_ `widths` or `heights` emit a value, but only after both `widths` and `heights` had emitted at least once.

### zip-by

```java
EventStream<Double> widths = ...;
EventStream<Double> heights = ...;
EventStream<Double> areas = EventStreams.zip(widths, heights).by((w, h) -> w * h);
```

`areas` emits a combined value every time _both_ `widths` and `heights` emit a value. Zip-by expects all input streams to emit values at the same frequency. In the above example, it would be an `IllegalStateException` if `widths` emitted twice while `heights` did not emit at all.


### combine-on-by

Emits a combined value, but only when the designated stream (impulse) emits a value.

```java
EventStream<Double> widths = ...;
EventStream<Double> heights = ...;
EventStream<Void> impulse = ...;
EventStream<Double> areas = EventStreams.combine(widths, heights).on(impulse).by((w, h) -> w * h);
```

The `areas` stream emits every time `impulse` emits, but only after both `widths` and `heights` had emitted at least once.


### emit-on

```java
EventStream<T> input = ...;
EventStream<?> impulse = ...;
EventStream stream = EventStreams.emit(input).on(impulse);
```

When `impulse` emits any value, `stream` emits the latest value emitted from `input`. If `input` did not emit any value between two emits from `impulse`, `stream` does not emit anything after the second impulse in a row.


Laziness of composite streams
-----------------------------

All the adapters and combinators above subscribe _lazily_ to their inputs - they don't subscribe to their inputs until they themselves have at least one subscriber. When the last subscriber unsubscribes, they unsubscribe from the inputs as well. This behavior has two benefits:
  1. unnecessary computation is avoided;
  2. composite stream's inputs don't prevent it from being garbage collected (no weak listeners needed).

Notice the difference to composed bindings. Bindings have to keep listening to their inputs all the time, because you can ask for the binding's current value (`Binding.getValue()`) any time. There is no such thing as the current value (event) of an event stream. This fact allows to automatically disconnect from the inputs when there are no subscribers.


Conversion to ObservableValue
-----------------------------

Every event stream can be converted to an `ObservableValue` that reflects the latest event emitted from the stream.

```java
EventStream<T> stream = ...;
T initial = ...;
ObservableValue<T> observable = stream.toObservableValue(initial);
```

`initial` is used as the value of `observable` until `stream` emits the first value.

Note that in the code above, a subscription to `stream` is created, but we don't have a reference to it, so we are unable to stop the subscription when `observable` is no longer needed. In fact, `toObservableValue()` returns a `StreamBoundValue`, which is an `ObservableValue` and a `Subscription` at the same time. The proper way to unsubscribe from the stream when `observable` is no longer used would be:

```java
StreamBoundValue<T> observable = stream.toObservableValue(initial);
// ...
observable.unsubscribe();
```


Interceptable streams
---------------------

`InterceptableEventStream` is an event stream whose event emission can be temporarily intercepted. `EventStream` provides the method `interceptable()` that returns an interceptable version of the event stream.

```java
EventStream<T> stream = ...;
InterceptableEventStream iStream = stream.interceptable();
```

`InterceptableEventStream` provides multiple ways to intercept the emission of events. They differ in what gets emitted when the interception ends.

Examples in the rest of this section build up on this code:

```java
EventSource<Integer> src = new EventSource<>();
InterceptableEventStream<Integer> iStream = src.interceptable();
iStream.subscribe(i -> System.out.println(i));
```

### mute

If you mute a stream temporarily, all events that would normally be emitted during this period are lost.

```java
iStream.muteWhile(() -> {
    src.push(1); // nothing is printed, 1 is never emitted from iStream
});
```


### pause

When you pause the stream, events that would normally be emitted are buffered and emitted when the stream is unpaused.

```java
iStream.pauseWhile(() -> {
    src.push(2);
    src.push(3);
    // nothing has been printed so far
});
// now "2" and "3" get printed
```


### retainLatest

Instructs the stream to remember only the latest event that would normally be emitted. This event is emitted when the interception ends.

```java
iStream.retainLatestWhile(() -> {
    src.push(4);
    src.push(5);
    // nothing has been printed so far
});
// now "5" gets printed
```


### fuse

While intercepted, keep _fusing_ (accumulating) the events together. One fused event is emitted when the interception ends.

```java
iStream.fuseWhile((a, b) -> a + b, () -> {
    src.push(6);
    src.push(7);
    src.push(8);
    // nothing has been printed so far
});
// now "21" gets printed
```

Note that `fuseWhile((a, b) -> b, runnable)` is equivalent to `retainLatestWhile(runnable)`.


### try fuse

Sometimes fusion is not defined for every pair of events. Sometimes two events _annihilate_ (cancel each other out). This type of interception tries to fuse or annihilate events when possible, and retains both events for later emmision when not possible. In the following example, two integers fuse (here add up) if their sum is less than 20 and annihilate if their sum is 0.

```java
BiFunction<Integer, Integer, FusionResult<Integer>> fusor = (a, b) -> {
    if(a + b == 0) {
        return FusionResult.annihilated();
    } else if(a + b < 20) {
        return FusionResult.fused(a + b);
    } else {
        return FusionResult.failed();
    }
};

iStream.fuseWhile(fusor, () -> {
    src.push(9);
    src.push(10);
    src.push(11);
    src.push(-5);
    src.push(-6);
    src.push(12);
    // nothing has been printed so far
});
// now "19" and "12" get printed
```

Note that `fuseWhile((a, b) -> FusionResult.failed(), runnable)` is equivalent to `pauseWhile(runnable)`.


InhiBeans
---------

InhiBeans are extensions of bindings and properties from `javafx.beans.*` that help prevent redundant invalidations and recalculations.

See [InhiBeans wiki page](https://github.com/TomasMikula/ReactFX/wiki/InhiBeans) for details.


Indicator
---------

`Indicator` is an observable boolean value that can be turned on temporarily.

```java
Indicator workBeingDone = new Indicator();
Runnable work = ...;
workBeingDone.onWhile(work);
```

A useful use case for indicator is to signal when a component is changing state.

Consider a rectangle that needs to be repainted every time its width or height changes.

```java
interface Rectangle {
    ObservableDoubleValue widthProperty();
    ObservableDoubleValue heightProperty();
    void setWidth(double width);
    void setHeight(double height);
}

Rectangle rect = ...;
rect.widthProperty().addListener(w -> repaint());
rect.heightProperty().addListener(h -> repaint());

rect.setWidth(20.0); // repaint #1
rect.setHeight(40.0); // repaint #2
```

Using indicator and stream combinators we can reduce the number of repaints in the above example to 1.

```java
interface Rectangle {
    ObservableDoubleValue widthProperty();
    ObservableDoubleValue heightProperty();
    Indicator beingUpdatedProperty();
    
    // put implementation of setWidth() and setHeight() inside
    // beingUpdatedProperty().onWhile(/* implementation */);
    void setWidth(double width);
    void setHeight(double height);
}

Rectangle rect = ...;
EventStream<Void> widthInvalidations = EventStreams.invalidationsOf(rect.widthProperty());
EventStream<Void> heightInvalidations = EventStreams.invalidationsOf(rect.heightProperty());
EventStream<Void> needsRepaint = EventStreams.merge(widthInvalidations, heightInvalidations);
EventStream<Void> doneUpdating = EventStreams.valuesOf(beingUpdatedProperty()).filter(updating -> !updating).map(updating -> null);
EventStream<Void> repaintImpulse = EventStreams.emit(needsRepaint).on(doneUpdating);
repaintImpulse.subscribe(i -> repaint());

rect.beingUpdatedProperty().onWhile(() -> {
    rect.setWidth(20.0);
    rect.setHeight(40.0);
});
// just 1 repaint takes place now
```


Links
-----

[Download](https://googledrive.com/host/0B4a5AnNnZhkbX0d4QUZXenRUaVE/downloads/)  
[Javadoc](https://googledrive.com/host/0B4a5AnNnZhkbX0d4QUZXenRUaVE/javadoc/index.html)  


License
-------

[BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)
