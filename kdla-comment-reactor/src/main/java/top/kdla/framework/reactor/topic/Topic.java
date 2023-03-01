package top.kdla.framework.reactor.topic;

import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.function.Consumer4;
import reactor.function.Consumer5;
import top.kdla.framework.reactor.help.RecyclableDequeue;
import top.kdla.framework.reactor.utils.RecyclerUtils;
import top.kdla.framework.reactor.utils.TopicUtils;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@EqualsAndHashCode(of = "part")
public final class Topic<T> {

    @Getter
    private final Topic<T> parent;

    @Setter(AccessLevel.PRIVATE)
    private String part;

    @Setter(AccessLevel.PRIVATE)
    private volatile String topic;

    @Setter(AccessLevel.PRIVATE)
    private volatile String[] topics;

    private final int depth;

    private final ConcurrentMap<String, Topic<T>> child = Maps.newConcurrentMap();

    private final ConcurrentMap<T, AtomicInteger> subscribers = Maps.newConcurrentMap();

    public static <T> Topic<T> createRoot() {
        return new Topic<>(null, "/");
    }

    public Topic<T> append(String topic) {
        if (topic.equals("/") || topic.equals("")) {
            return this;
        }
        return getOrDefault(topic, Topic::new);
    }

    private Topic(Topic<T> parent, String part) {

        if (StringUtils.isEmpty(part) || part.equals("/")) {
            this.part = "";
        } else {
            if (part.contains("/")) {
                this.ofTopic(part);
            } else {
                this.part = part;
            }
        }
        this.parent = parent;
        if (null != parent) {
            this.depth = parent.depth + 1;
        } else {
            this.depth = 0;
        }
    }

    public String[] getTopics() {
        if (topics != null) {
            return topics;
        }
        return topics = TopicUtils.split(getTopic());
    }

    public String getTopic() {
        if (topic == null) {
            Topic<T> parent = getParent();
            StringBuilder builder = new StringBuilder();
            if (parent != null) {
                String parentTopic = parent.getTopic();
                builder.append(parentTopic).append(parentTopic.equals("/") ? "" : "/");
            } else {
                builder.append("/");
            }
            return topic = builder.append(part).toString();
        }
        return topic;
    }

    public T getSubscriberOrSubscribe(Supplier<T> supplier) {
        if (subscribers.size() > 0) {
            return subscribers.keySet().iterator().next();
        }
        synchronized (this) {
            if (subscribers.size() > 0) {
                return subscribers.keySet().iterator().next();
            }
            T sub = supplier.get();
            subscribe(sub);
            return sub;
        }
    }

    public Set<T> getSubscribers() {
        return subscribers.keySet();
    }

    public boolean subscribed(T subscriber) {
        return subscribers.containsKey(subscriber);
    }

    @SafeVarargs
    public final void subscribe(T... subscribers) {
        for (T subscriber : subscribers) {
            this.subscribers.computeIfAbsent(subscriber, i -> new AtomicInteger()).incrementAndGet();
        }
    }

    @SafeVarargs
    public final List<T> unsubscribe(T... subscribers) {
        List<T> unsub = new ArrayList<>();
        for (T subscriber : subscribers) {
            this.subscribers.computeIfPresent(subscriber, (k, v) -> {
                if (v.decrementAndGet() <= 0) {
                    unsub.add(k);
                    return null;
                }
                return v;
            });
        }
        return unsub;
    }

    public final void unsubscribe(Predicate<T> predicate) {
        for (Map.Entry<T, AtomicInteger> entry : this.subscribers.entrySet()) {
            if (predicate.test(entry.getKey()) && entry.getValue().decrementAndGet() <= 0) {
                this.subscribers.remove(entry.getKey());
            }
        }
    }

    public final void unsubscribeAll() {
        this.subscribers.clear();
    }

    public Collection<Topic<T>> getChildren() {
        return child.values();
    }

    private void ofTopic(String topic) {
        String[] parts = topic.split("/", 2);
        this.part = parts[0];
        if (parts.length > 1) {
            Topic<T> part = new Topic<>(this, parts[1]);
            this.child.put(part.part, part);
        }
    }

    private Topic<T> getOrDefault(String topic, BiFunction<Topic<T>, String, Topic<T>> mapping) {
        if (topic.startsWith("/")) {
            topic = topic.substring(1);
        }
        String[] parts = topic.split("/");
        Topic<T> part = child.computeIfAbsent(parts[0], _topic -> mapping.apply(this, _topic));
        for (int i = 1; i < parts.length && part != null; i++) {
            Topic<T> parent = part;
            part = part.child.computeIfAbsent(parts[i], _topic -> mapping.apply(parent, _topic));
        }
        return part;
    }

    public Optional<Topic<T>> getTopic(String topic) {
        return Optional.ofNullable(getOrDefault(topic, ((topicPart, s) -> null)));
    }

    public Flux<Topic<T>> findTopic(String topic) {
        return Flux.create(sink -> {
            findTopic(topic, sink::next, sink::complete);
        });
    }

    public void findTopic(String topic,
                          Consumer<Topic<T>> sink,
                          Runnable end) {
        findTopic(topic,
                  null,
                  null,
                  end,
                  sink,
                  (nil, nil2, _end, _sink, _topic) -> _sink.accept(_topic),
                  (nil ,nil2, _end, _sink) -> _end.run());
    }

    public <ARG0, ARG1, ARG2, ARG3> void findTopic(String topic,
                                                   ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
                                                   Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
                                                   Consumer4<ARG0, ARG1, ARG2, ARG3> end) {
        String[] topics = TopicUtils.split(topic, true);

        if (!topic.startsWith("/")) {
            String[] newTopics = new String[topics.length + 1];
            newTopics[0] = "";
            System.arraycopy(topics, 0, newTopics, 1, topics.length);
            topics = newTopics;
        }

        find(topics, this, arg0, arg1, arg2, arg3, sink, end);
    }

    @Override
    public String toString() {
        return "topic: " + getTopic() + ", subscribers: " + subscribers.size() + ", children: " + child.size();
    }

    protected boolean match(String[] pars) {
        return TopicUtils.match(getTopics(), pars)
                || TopicUtils.match(pars, getTopics());
    }

    public static <T, ARG0, ARG1, ARG2, ARG3> void find(
            String[] topicParts,
            Topic<T> topicPart,
            ARG0 arg0, ARG1 arg1, ARG2 arg2, ARG3 arg3,
            Consumer5<ARG0, ARG1, ARG2, ARG3, Topic<T>> sink,
            Consumer4<ARG0, ARG1, ARG2, ARG3> end) {

        RecyclableDequeue<Topic<T>> cache = RecyclerUtils.dequeue();
        try {
            cache.add(topicPart);

            String nextPart = null;

            while (!cache.isEmpty()) {
                Topic<T> part = cache.poll();
                if (part == null) {
                    break;
                }
                if (part.match(topicParts)) {
                    sink.accept(arg0, arg1, arg2, arg3, part);
                }
                //订阅了如 /device/**/event/*
                if (part.part.equals("**")) {
                    Topic<T> tmp = null;
                    for (int i = part.depth; i < topicParts.length; i++) {
                        tmp = part.child.get(topicParts[i]);
                        if (tmp != null) {
                            cache.add(tmp);
                        }
                    }
                    if (null != tmp) {
                        continue;
                    }
                }
                if ("**".equals(nextPart) || "*".equals(nextPart)) {
                    cache.addAll(part.child.values());
                    continue;
                }
                Topic<T> next = part.child.get("**");
                if (next != null) {
                    cache.add(next);
                }
                next = part.child.get("*");
                if (next != null) {
                    cache.add(next);
                }

                if (part.depth + 1 >= topicParts.length) {
                    continue;
                }
                nextPart = topicParts[part.depth + 1];
                if (nextPart.equals("*") || nextPart.equals("**")) {
                    cache.addAll(part.child.values());
                    continue;
                }
                next = part.child.get(nextPart);
                if (next != null) {
                    cache.add(next);
                }
            }

            end.accept(arg0, arg1, arg2, arg3);
        } finally {
            cache.recycle();
        }
    }

    public long getTotalTopic() {
        long total = child.size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalTopic();
        }
        return total;
    }

    public long getTotalSubscriber() {
        long total = subscribers.size();
        for (Topic<T> tTopic : getChildren()) {
            total += tTopic.getTotalTopic();
        }
        return total;
    }

    public Flux<Topic<T>> getAllSubscriber() {
        List<Flux<Topic<T>>> all = new ArrayList<>();

        all.add(Flux.fromIterable(this.getChildren()));

        for (Topic<T> tTopic : getChildren()) {
            all.add(tTopic.getAllSubscriber());
        }
        return Flux.concat(all);
    }

    public void clean() {
        unsubscribeAll();
        getChildren().forEach(Topic::clean);
        child.clear();
    }

}
