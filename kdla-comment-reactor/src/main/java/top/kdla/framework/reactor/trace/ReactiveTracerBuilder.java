package top.kdla.framework.reactor.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import reactor.util.context.ContextView;

import javax.validation.constraints.NotBlank;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 响应式跟踪构造器
 *
 * @param <T> 跟踪器类型
 * @param <E> 响应式流元素类型
 * @see FluxTracer
 * @see MonoTracer
 */
public interface ReactiveTracerBuilder<T, E> {

    /**
     * 作用域,通常是库,包名或者类名
     *
     * @param name 作用域名
     * @return this
     * @see OpenTelemetry#getTracer(String)
     */
    ReactiveTracerBuilder<T, E> scopeName(@NotBlank String name);

    /**
     * 定义跟踪名称
     *
     * @param name 名称
     * @return this
     */
    ReactiveTracerBuilder<T, E> spanName(@NotBlank String name);

    /**
     * 监听流中的数据,并进行span自定义. 当流中产生数据时,回调函数被调用.
     *
     * @param callback 回调
     * @return this
     * @see Span
     * @see reactor.core.publisher.Flux#doOnNext(Consumer)
     */
    ReactiveTracerBuilder<T, E> onNext(BiConsumer<Span, E> callback);

    /**
     * 监听流完成,流完成时,回调函数被调用
     *
     * @param callback 回调函数
     * @return this
     * @see reactor.core.publisher.Flux#doOnComplete(Runnable)
     */
    ReactiveTracerBuilder<T, E> onComplete(BiConsumer<Span, Long> callback);

    /**
     * 当流被订阅时,回调函数被调用,可以使用回调中的上下文以及span builder来进行自定义.
     *
     * @param callback 回调函数
     * @return this
     */
    ReactiveTracerBuilder<T, E> onSubscription(BiConsumer<ContextView, SpanBuilder> callback);

    /**
     * 当流被订阅时,回调函数被调用,可以对span builder进行自定义.
     *
     * @param callback 回调函数
     * @return this
     */
    ReactiveTracerBuilder<T, E> onSubscription(Consumer<SpanBuilder> callback);

    /**
     * 构造跟踪器
     *
     * @return 跟踪器
     */
    T build();
}
