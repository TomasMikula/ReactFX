package org.reactfx;

import static org.reactfx.util.LL.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.binding.Binding;

import org.reactfx.StateMachine.InitialState;
import org.reactfx.StateMachine.ObservableStateBuilder;
import org.reactfx.StateMachine.ObservableStateBuilderOn;
import org.reactfx.StateMachine.StatefulStreamBuilder;
import org.reactfx.StateMachine.StatefulStreamBuilderOn;
import org.reactfx.util.LL;
import org.reactfx.util.Tuple2;

/**
 * Provides flexibility to compose streams together in more complicated ways than those found in {@link EventStream}.
 *
 * <p>
 *     First, the machine must be initialized with an initial state via {@link #init(Object)}. Then, one begins to
 *     compose {@link EventStream}s together via {@code on(EventStream)} followed by one of three methods:
 *     {@link StatefulStreamBuilderOn#transition(BiFunction) transition(BiFunction)},
 *     {@link StatefulStreamBuilderOn#emit(BiFunction) emit(BiFunction)}, and
 *     {@link StatefulStreamBuilderOn#transmit(BiFunction) transmit(BiFunction)}. After
 *     a combination of {@code on(inputEventStream).transition/emit/transmit(BiFunction)}, one can call
 *     {@link StatefulStreamBuilder#toEventStream()}. For example,
 * </p>
 *
 * <pre><code>
 *     StateMachine
 *          // set the initial state
 *          .init(10)
 *
 *          // transition: sets the machine's state to the value returned by the BiFunction
 *          .on(mouseEvents)     .transition((me, state) -&gt; 10)
 *          .on(leftArrowEvents) .transition((lae, state) -&gt; state - 1)
 *          .on(rightArrowEvents).transition((rae, state) -&gt; state + 1)
 *
 *          // emission: emits the value returned by the BiFunction if it is present
 *          .on(upArrowEvents).emit((uae, state) -&gt;
 *              uae.isShiftDown()
 *                  ? Optional.of(state)
 *                  : Optional.empty())
 *
 *           // transmission: combo of transition and emission: sets state to Tuple2::get1 and emits Tuple2::get2
 *           .on(downArrowEvents).transmit((dae, state) -&gt;
 *                  Tuples.t(
 *                      0,  // state will be set to this value
 *                      Optional.of(state + calculateSomeValue(dae)) // final EventStream will emit this value
 *                  ))
 *
 *           // finish composition by turning machine into an EventStream
 *           .toEventStream();
 * </code></pre>
 *
 * <p>
 *     One can also only use the {@link ObservableStateBuilder ObservableState} on-transmit couples
 *     ({@link ObservableStateBuilder#on}, {@link ObservableStateBuilderOn#transmit}) to
 *     return a {@link ObservableStateBuilder#toStateStream}, whose emission occurs under different circumstances than
 *     {@link StatefulStreamBuilder#toEventStream}, or {@link ObservableStateBuilder#toObservableState} its
 *     {@link EventStream#toBinding(Object) binding-like counterpart}.
 * </p>
 */
public class StateMachine {
    public interface InitialState<S> {
        <I> ObservableStateBuilderOn<S, I> on(EventStream<I> input);
    }

    public interface ObservableStateBuilder<S> {

        /**
         * Indicates than when the {@code input} {@link EventStream} emits a value, one of the following
         * three methods' {@link BiFunction} should be called: {@link ObservableStateBuilderOn#transition(BiFunction)},
         * {@link ObservableStateBuilderOn#emit(BiFunction)}, or {@link ObservableStateBuilderOn#transmit(BiFunction)}.
         */
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

        /**
         * Indicates than when the {@code input} {@link EventStream} emits a value, one of the following
         * three methods' {@link BiFunction} should be called: {@link StatefulStreamBuilderOn#transition(BiFunction)},
         * {@link StatefulStreamBuilderOn#emit(BiFunction)}, or {@link StatefulStreamBuilderOn#transmit(BiFunction)}.
         */
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

        /**
         * When the given {@link EventStream} emits a value, sets the machine's initial state to
         * {@code f.apply(emittedEvent, state)}
         */
        ObservableStateBuilder<S> transition(BiFunction<? super S, ? super I, ? extends S> f);

        /**
         * When the given {@link EventStream} emits a value, calls {@code f.apply(emittedEvent, state)} and
         * emits the value stored in the {@link Optional} if it {@link Optional#isPresent()}.
         */
        <O> StatefulStreamBuilder<S, O> emit(BiFunction<? super S, ? super I, Optional<O>> f);

        /**
         * When the given {@link EventStream} emits a value, calls {@code f.apply(emittedEvent, state)} and
         * sets the state's value to {@link Tuple2#get1()} and gets the {@link Optional} via {@link Tuple2#get2()}
         * and emits the value stored in it if it {@link Optional#isPresent()}.
         */
        <O> StatefulStreamBuilder<S, O> transmit(BiFunction<? super S, ? super I, Tuple2<S, Optional<O>>> f);
    }

    public interface StatefulStreamBuilderOn<S, O, I> {

        /**
         * When the given {@link EventStream} emits a value, sets the machine's initial state to
         * {@code f.apply(emittedEvent, state)}
         */
        StatefulStreamBuilder<S, O> transition(BiFunction<? super S, ? super I, ? extends S> f);

        /**
         * When the given {@link EventStream} emits a value, calls {@code f.apply(emittedEvent, state)} and
         * emits the value stored in the {@link Optional} if it {@link Optional#isPresent()}.
         */
        StatefulStreamBuilder<S, O> emit(BiFunction<? super S, ? super I, Optional<O>> f);

        /**
         * When the given {@link EventStream} emits a value, calls {@code f.apply(emittedEvent, state)} and
         * sets the state's value to {@link Tuple2#get1()} and then gets the {@link Optional} via {@link Tuple2#get2()}
         * and emits the value stored in it if it {@link Optional#isPresent()}.
         */
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
    protected Subscription observeInputs() {
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
    protected Subscription observeInputs() {
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