ReactFX
=======

This project is an exploration of reactive programming techniques for JavaFX. A lot of inspiration is drawn from the [rxJava library](https://github.com/Netflix/RxJava/wiki) and the excellent [Principles of Reactive Programming](https://www.coursera.org/course/reactive) course. Since ReactFX is targetted specifically for JavaFX applications, its design and use can be significantly simpler than that of rxJava. Most importantly, all UI events in JavaFX applications are handled on the JavaFX application thread. Therefore, ReactFX does not need to worry about asynchrony, schedulers, etc.

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

### zip-by

```java
EventStream<Double> widths = ...;
EventStream<Double> heights = ...;
EventStream<Double> areas = EventStreams.zip(widths, heights).by((w, h) -> w * h);
```

Zip-by differs from combine-by in that zip only emits value after all input streams have emitted a value. Moreover, zip expects all input streams to emit values at the same frequency. In the above example, it would an `IllegalStateException` if `widths` emitted twice while `heights` did not emit at all.


### combine-on-by

Emits a combined value, but only when the designated stream (impulse) emits a value.

```java
EventStream<Double> widths = ...;
EventStream<Double> heights = ...;
EventStream<Void> doneUpdating = ...;
EventStream<Double> areas = EventStreams.combine(widths, heights).on(doneUpdating).by((w, h) -> w * h);
```

The `areas` stream emits every time `doneUpdating` emits, but only after both `widths` and `heights` had emitted at least once.


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



Download
--------

Download the JAR file from [here](https://googledrive.com/host/0B4a5AnNnZhkbX0d4QUZXenRUaVE/).


License
-------

[BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)
