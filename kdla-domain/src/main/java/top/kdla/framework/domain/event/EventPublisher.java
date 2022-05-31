package top.kdla.framework.domain.event;

/**
 * 领域事件发布类
 * @author kll
 * @since 2021/7/21
 */
public interface EventPublisher {

    /**
     * 发布事件到MQ
     * @param event
     */
    void publish(DomainEvent event);
}
