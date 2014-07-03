ReactFX
=======

This project is an exploration of reactive programming techniques for JavaFX. A lot of inspiration is drawn from the [rxJava library](https://github.com/Netflix/RxJava/wiki) and the excellent [Principles of Reactive Programming](https://www.coursera.org/course/reactive) course. Since ReactFX specifically targets JavaFX applications, its design and use can be significantly simpler than that of rxJava. Most importantly, all UI events in JavaFX applications are handled on the JavaFX application thread. Therefore, ReactFX does not need to worry about asynchrony, schedulers, etc.

Help and discussion
-------------------

Use [reactfx](http://stackoverflow.com/tags/reactfx) tag on StackOverflow to ask specific questions. For more general discussions about the design of ReactFX and reactive programming for JavaFX, use the [reactfx-dev mailing list](https://groups.google.com/forum/#!forum/reactfx-dev).

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

### Multi-valued streams

Multi-valued streams compensate for the lack of language support for tuples in Java. ReactFX has convenience classes for 2- and 3-valued streams, namely `BiEventStream` and `TriEventStream`. This allows you to write

```java
BiEventStream<A, B> eventStream = ...;
eventStream.subscribe((a, b) -> f(a, b));
```

instead of

```java
EventStream<Tuple2<A, B>> eventStream = ...;
eventStream.subscribe(tuple -> f(tuple._1, tuple._2));
```


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
EventStream<?> invalidations = EventStreams.invalidationsOf(observable);
EventStream<Change<T>> changes = EventStreams.changesOf(observable);
EventStream<T> values = EventStreams.valuesOf(observable);
EventStream<T> nonNullValues = EventStreams.nonNullValuesOf(observable);
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


### combine

```java
EventStream<Double> widths = ...;
EventStream<Double> heights = ...;
EventStream<Double> areas = EventStreams.combine(widths, heights).map((w, h) -> w * h);
```

`areas` emits a combined value every time _either_ `widths` or `heights` emit a value, but only after both `widths` and `heights` had emitted at least once.


### zip

```java
EventStream<Double> widths = ...;
EventStream<Double> heights = ...;
EventStream<Double> areas = EventStreams.zip(widths, heights).map((w, h) -> w * h);
```

`areas` emits a combined value every time _both_ `widths` and `heights` emit a value. `zip` expects all input streams to emit values at the same frequency. In the above example, it would be an `IllegalStateException` if `widths` emitted twice while `heights` did not emit at all.


### reduceSuccessions

Accumulates events emitted in close temporal succession into one.

```java
EventSource<Integer> source = new EventSource<>();
EventStream<Integer> accum = source.reduceSuccessions((a, b) -> a + b, Duration.ofMillis(200));

source.push(1);
source.push(2);
// wait 150ms
source.push(3);
// wait 150ms
source.push(4);
// wait 250ms
source.push(5);
// wait 250ms
```
In the above example, an event that is emitted no later than 200ms after the previous one is accumulated (added) to the previous one. `accum` emits these values: 10, 5.


### and more...

See the [JavaDoc](http://www.reactfx.org/javadoc/org/reactfx/EventStream.html) for more stream combinators.


Laziness of composite streams
-----------------------------

All the adapters and combinators above subscribe _lazily_ to their inputs - they don't subscribe to their inputs until they themselves have at least one subscriber. When the last subscriber unsubscribes, they unsubscribe from the inputs as well. This behavior has two benefits:
  1. unnecessary computation is avoided;
  2. composite stream's inputs don't prevent it from being garbage collected (no weak listeners needed).

Notice the difference to composed bindings. Bindings have to keep listening to their inputs all the time, because you can ask for the binding's current value (`Binding.getValue()`) any time. There is no such thing as the current value (event) of an event stream. This fact allows to automatically disconnect from the inputs when there are no subscribers.


Conversion to Binding
---------------------

Every event stream can be converted to a `Binding` that reflects the most recent event emitted from the stream.

```java
EventStream<T> stream = ...;
T initial = ...;
Binding<T> binding = stream.toBinding(initial);
```

`initial` is used as the value of `binding` until `stream` emits the first value.

`binding` maintains an active subscription to `stream` until its `dispose()` method is called.


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


### reduce

While intercepted, keep _reducing_ (accumulating) the events together. The result of reduction is emitted when the interception ends.

```java
iStream.reduceWhile((a, b) -> a + b, () -> {
    src.push(6);
    src.push(7);
    src.push(8);
    // nothing has been printed so far
});
// now "21" gets printed
```

Note that `reduceWhile((a, b) -> b, runnable)` is equivalent to `retainLatestWhile(runnable)`.


### tryReduce

Sometimes reduction is not defined for every pair of events. Sometimes two events _annihilate_ (cancel each other out). This type of interception tries to reduce or annihilate the events when possible, and retains both events for later emmision when not possible. In the following example, two integers reduce (here add up) if their sum is less than 20 and annihilate if their sum is 0.

```java
BiFunction<Integer, Integer, ReductionResult<Integer>> reduction = (a, b) -> {
    if(a + b == 0) {
        return ReductionResult.annihilated();
    } else if(a + b < 20) {
        return ReductionResult.reduced(a + b);
    } else {
        return ReductionResult.failed();
    }
};

iStream.tryReduceWhile(reduction, () -> {
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

Note that `tryReduceWhile((a, b) -> ReductionResult.failed(), runnable)` is equivalent to `pauseWhile(runnable)`.


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
EventStream<Void> doneUpdating = beingUpdatedProperty().offs();
EventStream<Void> repaintImpulse = EventStreams.emit(needsRepaint).on(doneUpdating);
repaintImpulse.subscribe(i -> repaint());

rect.beingUpdatedProperty().onWhile(() -> {
    rect.setWidth(20.0);
    rect.setHeight(40.0);
});
// just 1 repaint takes place now
```


Error handling
--------------

ReactFX has a mechanism to handle errors encountered by event streams. You can read more about this mechanism on the [Error Handling wiki page](https://github.com/TomasMikula/ReactFX/wiki/Error-Handling).


Use ReactFX in your project
---------------------------

### Stable release

Current stable release is 1.2.1.

#### Maven coordinates

| Group ID    | Artifact ID | Version |
| :---------: | :---------: | :-----: |
| org.reactfx | reactfx     | 1.2.1   |

#### Gradle example

```groovy
dependencies {
    compile group: 'org.reactfx', name: 'reactfx', version: '1.2.1'
}
```

#### Sbt example

```scala
libraryDependencies += "org.reactfx" % "reactfx" % "1.2.1"
```

#### Manual download

[Download](https://github.com/TomasMikula/ReactFX/releases/download/v1.2.1/reactfx-1.2.1.jar) the JAR file and place it on your classpath.


### Snapshot releases

Snapshot releases are deployed to Sonatype snapshot repository.

#### Maven coordinates

| Group ID    | Artifact ID | Version        |
| :---------: | :---------: | :------------: |
| org.reactfx | reactfx     | 1.2.2-SNAPSHOT |

#### Gradle example

```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots/' 
    }
}

dependencies {
    compile group: 'org.reactfx', name: 'reactfx', version: '1.2.2-SNAPSHOT'
}
```

#### Sbt example

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.reactfx" % "reactfx" % "1.2.2-SNAPSHOT"
```

#### Manual download

[Download](https://oss.sonatype.org/content/repositories/snapshots/org/reactfx/reactfx/1.2.2-SNAPSHOT/) the latest JAR file and place it on your classpath.


Links
-----

[API Documentation (Javadoc)](http://www.reactfx.org/javadoc/overview-summary.html)  


License
-------

[BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)
