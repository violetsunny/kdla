package top.kdla.framework.domain.event;

import lombok.Data;
import lombok.ToString;

/**
 * 领域事件基类
 *
 * @author vincent.li
 * @since 2021/7/21
 */
@ToString
@Data
public class DomainEvent {

    /**
     * 领域事件id
     */
    private String id;
    /**
     * 时间戳
     */
    private long timestamp;
    /**
     * 领域事件来源
     */
    private String source;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 数据
     */
    private String data;
}
