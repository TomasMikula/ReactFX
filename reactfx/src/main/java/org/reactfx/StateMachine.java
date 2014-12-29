package org.reactfx;

import static org.reactfx.LL.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javafx.beans.binding.Binding;

import org.reactfx.StateMachine.InitialState;
import org.reactfx.StateMachine.ObservableStateBuilder;
import org.reactfx.StateMachine.ObservableStateBuilderOn;
import org.reactfx.StateMachine.StatefulStreamBuilder;
import org.reactfx.StateMachine.StatefulStreamBuilderOn;
import org.reactfx.util.Tuple2;

public class StateMachine {
    public interface InitialState<S> {
        <I> ObservableStateBuilderOn<S, I> on(EventStream<I> input);
    }

    public interface ObservableStateBuilder<S> {
        <I> ObservableStateBuilderOn<S, I> on(EventStream<I> input);

        /**
         * Returns an event stream that emits the current state of the state
         * machine every time it changes.
         */
        EventStream<S> toStateStream();

        /**
         * Returns a binding that reflects the current state of the state
         * machine. Disposing the returned binding (by calling its
         * {@code dispose()} method) causes the state machine to unsubscribe
         * from the event streams that alter its state and allows the state
         * machine to be garbage collected.
         */
        Binding<S> toObservableState();
    }

    public interface StatefulStreamBuilder<S, O> {
        <I> StatefulStreamBuilderOn<S, O, I> on(EventStream<I> input);

        /**
         * Returns an event stream that emits a value when one of the state
         * machine's input streams causes the state machine to emit a value.
         *
         * <p>The returned event stream is <em>lazily bound</em>, meaning the
         * associated state machine is subscribed to its inputs only when the
         * returned stream has at least one subscriber. No state transitions
         * take place unless there is a subscriber to the returned stream. If
         * you need to keep the state machine alive even when temporarily not
         * subscribed to the returned stream, you can <em>pin</em> the returned
         * stream.
         */
        EventStream<O> toEventStream();
    }

    public interface ObservableStateBuilderOn<S, I> {
        ObservableStateBuilder<S> transition(BiFunction<? super S, ? super I, ? extends S> f);
        <O> StatefulStreamBuilder<S, O> emit(BiFunction<? super S, ? super I, Optional<O>> f);
        <O> StatefulStreamBuilder<S, O> transmit(BiFunction<? super S, ? super I, Tuple2<S, Optional<O>>> f);
    }

    public interface StatefulStreamBuilderOn<S, O, I> {
        StatefulStreamBuilder<S, O> transition(BiFunction<? super S, ? super I, ? extends S> f);
        StatefulStreamBuilder<S, O> emit(BiFunction<? super S, ? super I, Optional<O>> f);
        StatefulStreamBuilder<S, O> transmit(BiFunction<? super S, ? super I, Tuple2<S, Optional<O>>> f);
    }

    public static <S> InitialState<S> init(S initialState) {
        return new InitialStateImpl<>(initialState);
    }
}

class InitialStateImpl<S> implements InitialState<S> {
    private final S initialState;

    public InitialStateImpl(S initialState) {
        this.initialState = initialState;
    }

    @Override
    public <I> ObservableStateBuilderOn<S, I> on(EventStream<I> input) {
        return new ObservableStateBuilderOnImpl<>(initialState, nil(), input);
    }
}

class ObservableStateBuilderImpl<S> implements ObservableStateBuilder<S> {
    private final S initialState;
    private final LL<TransitionBuilder<S>> transitions;

    ObservableStateBuilderImpl(S initialState, LL<TransitionBuilder<S>> transitions) {
        this.initialState = initialState;
        this.transitions = transitions;
    }

    @Override
    public <I> ObservableStateBuilderOn<S, I> on(EventStream<I> input) {
        return new ObservableStateBuilderOnImpl<>(initialState, transitions, input);
    }

    @Override
    public EventStream<S> toStateStream() {
        return new StateStream<>(initialState, transitions);
    }

    @Override
    public Binding<S> toObservableState() {
        return toStateStream().toBinding(initialState);
    }
}

class StateStream<S> extends EventStreamBase<S> {
    private final InputHandler[] inputHandlers;

    private S state;

    public StateStream(S initialState, LL<TransitionBuilder<S>> transitions) {
        inputHandlers = transitions.stream()
                .map(t -> t.build(this::handleTransition))
                .toArray(n -> new InputHandler[n]);
        state = initialState;
    }

    @Override
    protected Subscription bindToInputs() {
        return Subscription.multi(
                InputHandler::subscribeToInput,
                inputHandlers);
    }

    private void handleTransition(Function<S, S> transition) {
        state = transition.apply(state);
        emit(state);
    }
}

class StatefulStreamBuilderImpl<S, O> implements StatefulStreamBuilder<S, O> {
    private final S initialState;
    private final LL<TransitionBuilder<S>> transitions;
    private final LL<EmissionBuilder<S, O>> emissions;
    private final LL<TransmissionBuilder<S, O>> transmissions;

    StatefulStreamBuilderImpl(
            S initialState,
            LL<TransitionBuilder<S>> transitions,
            LL<EmissionBuilder<S, O>> emissions,
            LL<TransmissionBuilder<S, O>> transmissions) {
        this.initialState = initialState;
        this.transitions = transitions;
        this.emissions = emissions;
        this.transmissions = transmissions;
    }

    @Override
    public <I> StatefulStreamBuilderOn<S, O, I> on(EventStream<I> input) {
        return new StatefulStreamBuilderOnImpl<>(initialState, transitions, emissions, transmissions, input);
    }

    @Override
    public EventStream<O> toEventStream() {
        return new StatefulStream<>(initialState, transitions, emissions, transmissions);
    }
}

class StatefulStream<S, O> extends EventStreamBase<O> {
    private final List<InputHandler> inputHandlers;

    private S state;

    StatefulStream(
            S initialState,
            LL<TransitionBuilder<S>> transitions,
            LL<EmissionBuilder<S, O>> emissions,
            LL<TransmissionBuilder<S, O>> transmissions) {

        state = initialState;

        this.inputHandlers = new ArrayList<>(transitions.size() + emissions.size() + transmissions.size());

        for(TransitionBuilder<S> tb: transitions) {
            inputHandlers.add(tb.build(this::handleTransition));
        }

        for(EmissionBuilder<S, O> eb: emissions) {
            inputHandlers.add(eb.build(this::handleEmission));
        }

        for(TransmissionBuilder<S, O> tb: transmissions) {
            inputHandlers.add(tb.build(this::handleTransmission));
        }
    }

    @Override
    protected Subscription bindToInputs() {
        return Subscription.multi(
                InputHandler::subscribeToInput,
                inputHandlers);
    }

    private void handleTransition(Function<S, S> transition) {
        state = transition.apply(state);
    }

    private void handleEmission(Function<S, Optional<O>> emission) {
        emission.apply(state).ifPresent(this::emit);
    }

    private void handleTransmission(Function<S, Tuple2<S, Optional<O>>> transmission) {
        Tuple2<S, Optional<O>> pair = transmission.apply(state);
        state = pair._1;
        pair._2.ifPresent(this::emit);
    }
}

class ObservableStateBuilderOnImpl<S, I> implements ObservableStateBuilderOn<S, I> {
    private final S initialState;
    private final LL<TransitionBuilder<S>> transitions;
    private final EventStream<I> input;

    ObservableStateBuilderOnImpl(
            S initialState,
            LL<TransitionBuilder<S>> transitions,
            EventStream<I> input) {
        this.initialState = initialState;
        this.transitions = transitions;
        this.input = input;
    }

    @Override
    public ObservableStateBuilder<S> transition(
            BiFunction<? super S, ? super I, ? extends S> f) {
        TransitionBuilder<S> transition = new TransitionBuilder<>(input, f);
        return new ObservableStateBuilderImpl<>(initialState, cons(transition, transitions));
    }

    @Override
    public <O> StatefulStreamBuilder<S, O> emit(
            BiFunction<? super S, ? super I, Optional<O>> f) {
        EmissionBuilder<S, O> emission = new EmissionBuilder<>(input, f);
        return new StatefulStreamBuilderImpl<>(initialState, transitions, cons(emission, nil()), nil());
    }

    @Override
    public <O> StatefulStreamBuilder<S, O> transmit(
            BiFunction<? super S, ? super I, Tuple2<S, Optional<O>>> f) {
        TransmissionBuilder<S, O> transmission = new TransmissionBuilder<>(input, f);
        return new StatefulStreamBuilderImpl<>(initialState, transitions, nil(), cons(transmission, nil()));
    }
}

class StatefulStreamBuilderOnImpl<S, O, I> implements StatefulStreamBuilderOn<S, O, I> {
    private final S initialState;
    private final LL<TransitionBuilder<S>> transitions;
    private final LL<EmissionBuilder<S, O>> emissions;
    private final LL<TransmissionBuilder<S, O>> transmissions;
    private final EventStream<I> input;

    StatefulStreamBuilderOnImpl(
            S initialState,
            LL<TransitionBuilder<S>> transitions,
            LL<EmissionBuilder<S, O>> emissions,
            LL<TransmissionBuilder<S, O>> transmissions,
            EventStream<I> input) {
        this.initialState = initialState;
        this.transitions = transitions;
        this.emissions = emissions;
        this.transmissions = transmissions;
        this.input = input;
    }

    @Override
    public StatefulStreamBuilder<S, O> transition(
            BiFunction<? super S, ? super I, ? extends S> f) {
        TransitionBuilder<S> transition = new TransitionBuilder<>(input, f);
        return new StatefulStreamBuilderImpl<>(initialState, cons(transition, transitions), emissions, transmissions);
    }

    @Override
    public StatefulStreamBuilder<S, O> emit(
            BiFunction<? super S, ? super I, Optional<O>> f) {
        EmissionBuilder<S, O> emission = new EmissionBuilder<>(input, f);
        return new StatefulStreamBuilderImpl<>(initialState, transitions, cons(emission, emissions), transmissions);
    }

    @Override
    public StatefulStreamBuilder<S, O> transmit(
            BiFunction<? super S, ? super I, Tuple2<S, Optional<O>>> f) {
        TransmissionBuilder<S, O> transmission = new TransmissionBuilder<>(input, f);
        return new StatefulStreamBuilderImpl<>(initialState, transitions, emissions, cons(transmission, transmissions));
    }

}

interface InputHandler {
    Subscription subscribeToInput();
}

class InputHandlerBuilder<S, TGT> {
    private final Function<
            Consumer<Function<S, TGT>>,
            InputHandler> inputSubscriberProvider;

    public <I> InputHandlerBuilder(EventStream<I> input,
            BiFunction<? super S, ? super I, ? extends TGT> f) {
        this.inputSubscriberProvider = publisher -> {
            return () -> input.subscribe(i -> publisher.accept(s -> f.apply(s, i)));
        };
    }

    public InputHandler build(Consumer<Function<S, TGT>> c) {
        return inputSubscriberProvider.apply(c);
    }
}

class TransitionBuilder<S> extends InputHandlerBuilder<S, S> {
    public <I> TransitionBuilder(EventStream<I> input,
            BiFunction<? super S, ? super I, ? extends S> f) {
        super(input, f);
    }
}

class EmissionBuilder<S, O> extends InputHandlerBuilder<S, Optional<O>> {
    public <I> EmissionBuilder(EventStream<I> input,
            BiFunction<? super S, ? super I, ? extends Optional<O>> f) {
        super(input, f);
    }
}

class TransmissionBuilder<S, O> extends InputHandlerBuilder<S, Tuple2<S, Optional<O>>> {
    public <I> TransmissionBuilder(EventStream<I> input,
            BiFunction<? super S, ? super I, ? extends Tuple2<S, Optional<O>>> f) {
        super(input, f);
    }
}

/**
 * Immutable linked list.
 */
interface LL<T> extends Iterable<T> {
    static <T> LL<T> nil() { return Nil.instance(); }
    static <T> LL<T> cons(T head, LL<T> tail) { return new NonEmpty<>(head, tail); }

    boolean isEmpty();
    int size();
    T head();
    LL<T> tail();

    default LL<T> prepend(T head) {
        return new NonEmpty<>(head, this);
    }

    @Override
    default Iterator<T> iterator() {
        return new Iterator<T>() {
            private LL<T> l = LL.this;

            @Override
            public boolean hasNext() {
                return !l.isEmpty();
            }

            @Override
            public T next() {
                T res = l.head();
                l = l.tail();
                return res;
            }
        };
    }

    default List<T> toList() {
        List<T> res = new ArrayList<>(size());
        for(LL<T> l = this; !l.isEmpty(); l = l.tail()) {
            res.add(l.head());
        }
        return res;
    }


    default Stream<T> stream() {
        Spliterator<T> spliterator = new Spliterator<T>() {
            private final Iterator<T> iterator = iterator();

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if(iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return size();
            }

            @Override
            public int characteristics() {
                return Spliterator.IMMUTABLE | Spliterator.SIZED;
            }
        };

        return StreamSupport.stream(spliterator, false);
    }
}

class Nil<T> implements LL<T> {
    private static final Nil<?> INSTANCE = new Nil<Void>();

    @SuppressWarnings("unchecked")
    static <T> Nil<T> instance() { return (Nil<T>) INSTANCE; }

    @Override public boolean isEmpty() { return true; }
    @Override public int size() { return 0; }
    @Override public T head() { throw new NoSuchElementException(); }
    @Override public LL<T> tail() { throw new NoSuchElementException(); }
}

class NonEmpty<T> implements LL<T> {
    private final T head;
    private final LL<T> tail;

    NonEmpty(T head, LL<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    private int size = -1;

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        if(size == -1) {
            size = 1 + tail.size();
        }
        return size;
    }

    @Override
    public T head() {
        return head;
    }

    @Override
    public LL<T> tail() {
        return tail;
    }
}