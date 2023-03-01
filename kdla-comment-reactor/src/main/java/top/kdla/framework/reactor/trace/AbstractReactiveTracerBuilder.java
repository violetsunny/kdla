package top.kdla.framework.reactor.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import reactor.util.context.ContextView;

import javax.validation.constraints.NotBlank;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class AbstractReactiveTracerBuilder<T, R> implements ReactiveTracerBuilder<T, R> {
    String scopeName;
    String spanName;
    BiConsumer<Span, R> onNext;
    BiConsumer<Span, Long> onComplete;
    BiConsumer<ContextView, SpanBuilder> onSubscription;

    @Override
    public ReactiveTracerBuilder<T, R> scopeName(@NotBlank String name) {
        this.scopeName = name;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> spanName(@NotBlank String name) {
        this.spanName = name;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onNext(BiConsumer<Span, R> callback) {
        this.onNext = callback;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onComplete(BiConsumer<Span, Long> callback) {
        this.onComplete = callback;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(BiConsumer<ContextView, SpanBuilder> callback) {
        this.onSubscription = callback;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(Consumer<SpanBuilder> callback) {
        this.onSubscription = callback == null ? null : (contextView, spanBuilder) -> callback.accept(spanBuilder);
        return this;
    }

    @Override
    public abstract T build();
}
