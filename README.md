ReactFX
=======

ReactFX is an exploration of (functional) reactive programming techniques for JavaFX. These techniques usually result in more concise code, less side effects and less inversion of control, all of which improve the readability of code.

Initial inspiration came from the [Principles of Reactive Programming](https://www.coursera.org/course/reactive) course and the [RxJava library](https://github.com/Netflix/RxJava/wiki). There are, however, important [differences from RxJava](https://github.com/TomasMikula/ReactFX/wiki/ReactFX-vs-ReactiveX).

Help and Discussion
-------------------

Use [reactfx](http://stackoverflow.com/tags/reactfx) tag on StackOverflow to ask specific questions. For more general discussions about the design of ReactFX and reactive programming for JavaFX, use the [reactfx-dev mailing list](https://groups.google.com/forum/#!forum/reactfx-dev).

Event Streams
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

Event Streams vs Observable Values
----------------------------------

JavaFX has a representation of a _time-varying value_, namely [ObservableValue](http://docs.oracle.com/javase/8/javafx/api/javafx/beans/value/ObservableValue.html). `ObservableValue` holds a value at any point in time. This value can be requested with [getValue()](http://docs.oracle.com/javase/8/javafx/api/javafx/beans/value/ObservableValue.html#getValue--).

Events, on the other hand, are _ephemeral_&mdash;they come and go. You can only be notified of an event when it occurs;&mdash;it does not make sense to ask the event stream about the _"current event"_.

JavaFX has means to compose observable values to form new observable values, either using the _fluent API_ (methods of ObservableValue subclasses), or using the [Bindings](http://docs.oracle.com/javase/8/javafx/api/javafx/beans/binding/Bindings.html) helper class. Some useful compositions of observable values are also provided by the [EasyBind](https://github.com/TomasMikula/EasyBind) library.

JavaFX, however, does not have a nice way to compose streams of events. The user is left with event handlers/listeners, which are not composable and inherently side-effectful. EventStreams try to fill this gap.


Event Streams in JavaFX
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

See the [JavaDoc](http://www.reactfx.org/javadoc/stable/org/reactfx/EventStream.html) for more stream combinators.


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


Suspendable streams
-------------------

`SuspendableEventStream` is an event stream whose event emission can be temporarily suspended. There are several types of suspendable event streams that differ in what events, if any, are emitted when their emission is resumed.

### suppressible

When a suppressible stream is suspended, all events that would normally be emitted during this period are lost.

```java
EventSource<Integer> src = new EventSource<>();
SuspendableEventStream<Integer> stream = src.suppressible();
stream.subscribe(i -> System.out.println(i));
stream.suspendWhile(() -> {
    src.push(1); // nothing is printed, 1 is never emitted from stream
});
```


### pausable

When a pausable stream is suspended, events that would normally be emitted are buffered and emitted when event emission is resumed.

```java
EventSource<Integer> src = new EventSource<>();
SuspendableEventStream<Integer> stream = src.pausable();
stream.subscribe(i -> System.out.println(i));
stream.suspendWhile(() -> {
    src.push(2);
    src.push(3);
    // nothing has been printed so far
});
// now "2" and "3" get printed
```


### forgetful

When a forgetful stream is suspended, only the latest event that would normally be emitted is remembered. This event is emitted when event emission is resumed.

```java
EventSource<Integer> src = new EventSource<>();
SuspendableEventStream<Integer> stream = src.forgetful();
stream.subscribe(i -> System.out.println(i));
stream.suspendWhile(() -> {
    src.push(4);
    src.push(5);
    // nothing has been printed so far
});
// now "5" gets printed
```


### reducible

When a reducible stream is suspended, it keeps _reducing_ the incoming events together. The result of reduction is emitted when event emission is resumed.

```java
EventSource<Integer> src = new EventSource<>();
SuspendableEventStream<Integer> stream = src.reducible((a, b) -> a + b);
stream.subscribe(i -> System.out.println(i));
stream.suspendWhile(() -> {
    src.push(6);
    src.push(7);
    src.push(8);
    // nothing has been printed so far
});
// now "21" gets printed
```

Note that `forgetful()` is equivalent to `reducible((a, b) -> b)`.


### accumulative

When an accumulative stream is suspended, it keeps _accumulating_ the incoming events into a cumulative value (accumulator), which may be of a different type than the events. When event emission is resumed, the accumulated value is _deconstructed_ into a sequence of events that are emitted from the stream. This is a generalization of all previous suspendable streams.

`reducible(reduction)` can be modeled like this:

```java
accumulative(t -> t, reduction, t -> Collections.singletonList(t))
```

`suppressible()` can be modeled like this:

```java
accumulative(t -> (Void) null, (a, t) -> a, a -> Collections.emptyList())
```

`pausable()` can be modeled like this:

```java
accumulative(ArrayList<T>::new, (l, t) -> { l.add(t); return l; }, l -> l)
```


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
EventStream<?> widthInvalidations = EventStreams.invalidationsOf(rect.widthProperty());
EventStream<?> heightInvalidations = EventStreams.invalidationsOf(rect.heightProperty());
EventStream<?> needsRepaint = EventStreams.merge(widthInvalidations, heightInvalidations);
EventStream<?> doneUpdating = beingUpdatedProperty().offs();
EventStream<?> repaintImpulse = needsRepaint.emitOn(doneUpdating);
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

Current stable release is 1.4.1.

#### Maven coordinates

| Group ID    | Artifact ID | Version |
| :---------: | :---------: | :-----: |
| org.reactfx | reactfx     | 1.4.1   |

#### Gradle example

```groovy
dependencies {
    compile group: 'org.reactfx', name: 'reactfx', version: '1.4.1'
}
```

#### Sbt example

```scala
libraryDependencies += "org.reactfx" % "reactfx" % "1.4.1"
```

#### Manual download

[Download](https://github.com/TomasMikula/ReactFX/releases/download/v1.4.1/reactfx-1.4.1.jar) the JAR file and place it on your classpath.


### Milestone release

Current milestone release is 2.0-M5.

#### Maven coordinates

| Group ID    | Artifact ID | Version |
| :---------: | :---------: | :-----: |
| org.reactfx | reactfx     | 2.0-M5  |

#### Gradle example

```groovy
dependencies {
    compile group: 'org.reactfx', name: 'reactfx', version: '2.0-M5'
}
```

#### Sbt example

```scala
libraryDependencies += "org.reactfx" % "reactfx" % "2.0-M5"
```

#### Manual download

[Download](https://github.com/TomasMikula/ReactFX/releases/download/v2.0-M5/reactfx-2.0-M5.jar) the JAR file and place it on your classpath.


### Snapshot releases

Snapshot releases are deployed to Sonatype snapshot repository.

#### Maven coordinates

| Group ID    | Artifact ID | Version        |
| :---------: | :---------: | :------------: |
| org.reactfx | reactfx     | 2.0-SNAPSHOT   |

#### Gradle example

```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots/' 
    }
}

dependencies {
    compile group: 'org.reactfx', name: 'reactfx', version: '2.0-SNAPSHOT'
}
```

#### Sbt example

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.reactfx" % "reactfx" % "2.0-SNAPSHOT"
```

#### Manual download

[Download](https://oss.sonatype.org/content/repositories/snapshots/org/reactfx/reactfx/2.0-SNAPSHOT/) the latest JAR file and place it on your classpath.


Links
-----

[API 1.4.1 (Javadoc)](http://www.reactfx.org/javadoc/1.4.1/overview-summary.html)  
[API 2.0-M5 (Javadoc)](http://www.reactfx.org/javadoc/2.0-M5/overview-summary.html)  


License
-------

[BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)
