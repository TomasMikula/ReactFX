package org.reactfx;

/**
 * An EventStream that does not emit the next event if the trigger stream emits
 * an event first.
 */
public class SwallowOnStream<T> extends LazilyBoundStream<T> {
	private EventStream<T> source;
	private EventStream<?> trigger;

	private boolean swallowNext = false;

	public SwallowOnStream(EventStream<T> source, EventStream<?> trigger) {
		this.source = source;
		this.trigger = trigger;
	}

	@Override
	protected void emit(T value) {
		if (swallowNext) {
			swallowNext = false;
		}
		else {
			super.emit(value);
		}
	}

	@Override
	protected Subscription subscribeToInputs() {
		Subscription s1 = subscribeTo(trigger, this::suspendSource);
		Subscription s2 = subscribeTo(source, this::emit);

		return s1.and(s2);
	}

	private void suspendSource(Object ignore) {
		swallowNext = true;
	}

}
