package top.kdla.framework.domain.event;

/**
 * 领域事件处理类
 * @author vincent.li
 * @since 2021/7/21
 */
public interface EventHandler {

    /**
     * 处理领域事件
     * @param event
     */
    void handle(DomainEvent event);
}
