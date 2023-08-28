package top.kdla.framework.reactor.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.BiConsumer;

class TraceSubscriber<T> extends BaseSubscriber<T> implements Span {

    final static AttributeKey<Long> COUNT = AttributeKey.longKey("count");

    @SuppressWarnings("all")
    final static AtomicLongFieldUpdater<TraceSubscriber> NEXT_COUNT = AtomicLongFieldUpdater
            .newUpdater(TraceSubscriber.class, "nextCount");

    private final CoreSubscriber<? super T> actual;
    private final Span span;
    private final BiConsumer<Span, T> onNext;
    private final BiConsumer<Span, Long> onComplete;


    private volatile long nextCount;
    private volatile boolean stateSet;
    private final Context context;

    public TraceSubscriber(CoreSubscriber<? super T> actual,
                           Span span,
                           BiConsumer<Span, T> onNext,
                           BiConsumer<Span, Long> onComplete,
                           io.opentelemetry.context.Context ctx) {
        this.actual = actual;
        this.span = span;
        this.onNext = onNext;
        this.onComplete = onComplete;
        this.context = Context
                .of(actual.currentContext())
                .put(SpanContext.class, span.getSpanContext())
                .put(io.opentelemetry.context.Context.class, span.storeInContext(ctx));
    }

    @Override
    protected void hookOnSubscribe(@NotNull Subscription subscription) {
        actual.onSubscribe(this);
    }

    @Override
    protected void hookOnError(@NotNull Throwable throwable) {
        span.setStatus(StatusCode.ERROR);
        span.recordException(throwable);
        try (Scope scope = span.makeCurrent()) {
            actual.onError(throwable);
        }
    }

    @Override
    @NotNull
    public Context currentContext() {
        return context;
    }

    @Override
    protected void hookFinally(@NotNull SignalType type) {
        span.end();
    }

    @Override
    protected void hookOnCancel() {
        if (!stateSet) {
            span.setStatus(StatusCode.ERROR, "cancel");
        }
    }

    @Override
    protected void hookOnNext(@NotNull T value) {
        if (null != onNext) {
            onNext.accept(this, value);
        }
        NEXT_COUNT.incrementAndGet(this);
        try (Scope scope = span.makeCurrent()) {
            actual.onNext(value);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (onComplete != null) {
            onComplete.accept(this, nextCount);
        }
        span.setAttribute(COUNT, nextCount);
        if (!stateSet) {
            span.setStatus(StatusCode.OK);
        }
        try (Scope scope = span.makeCurrent()) {
            actual.onComplete();
        }
    }

    @Override
    public <R> Span setAttribute(@NotNull AttributeKey<R> key, @NotNull R value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public Span addEvent(@NotNull String name, @NotNull Attributes attributes) {
        span.addEvent(name, attributes);
        return this;
    }

    @Override
    public Span addEvent(@NotNull String name, @NotNull Attributes attributes, long timestamp, @NotNull TimeUnit unit) {
        span.addEvent(name, attributes, timestamp, unit);
        return this;
    }

    @Override
    public Span setStatus(@NotNull StatusCode statusCode, @NotNull String description) {
        stateSet = true;
        span.setStatus(statusCode, description);
        return this;
    }

    @Override
    public Span recordException(@NotNull Throwable exception, @NotNull Attributes additionalAttributes) {
        span.recordException(exception, additionalAttributes);
        return this;
    }

    @Override
    public Span updateName(@NotNull String name) {
        span.updateName(name);
        return this;
    }

    @Override
    public void end() {
        //do nothing
    }

    @Override
    public void end(long timestamp, @NotNull TimeUnit unit) {
        //do nothing
    }

    @Override
    public SpanContext getSpanContext() {
        return span.getSpanContext();
    }

    @Override
    public boolean isRecording() {
        return span.isRecording();
    }
}