/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.reactor.help;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author kanglele
 * @version $Id: MultiThreadReactorHelp, v 0.1 2023/2/21 16:50 kanglele Exp $
 */
@Slf4j
public class MultiThreadReactorHelp<T, R> {

    public R exe(List<T> reqs, Function<? super T, ? extends Publisher<? extends R>> function, ExecutorService executorService) {
        long startTime = System.currentTimeMillis();
        AtomicReference<R> res = new AtomicReference<>();
        Flux.fromIterable(reqs)
                .parallel()
                .runOn(Schedulers.fromExecutorService(executorService))
                .flatMap(function)
                //.flatMap(key -> doRpcUseWebClient(key))
                .sequential()
                .doOnError(MultiThreadReactorHelp::doOnError)
                .doOnComplete(MultiThreadReactorHelp::doOnComplete)
                .doFinally(signalType -> {
                    if (log.isInfoEnabled()) {
                        log.info("并发执行的时间: " + (System.currentTimeMillis() - startTime));
                    }
                })
                .subscribe(responseData -> res.set(responseData), e -> log.error("error", e));

        return res.get();
    }

    private static void doOnError(Object o) {
        log.error("{}", o);
    }


    private static void doOnComplete() {
        if (log.isInfoEnabled()) {
            log.info("并发远程调用异常完成");
        }
    }

    /**
     * private Flux<RestOut<JSONObject>> doRpcUseWebClient(int key) {
     *     ....
     *     Mono<RestOut<JSONObject>> resp = retrieve.bodyToMono(parameterizedTypeReference);
     *     return Flux.from(resp);
     * }
     */

}
