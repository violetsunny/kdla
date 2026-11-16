package top.kdla.framework.infra.dal.es.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author shq
 */
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Data
public class EsSourceData extends EsBaseData {

    private static final long serialVersionUID = 5516075349620653480L;

    private long timestamp;

    private String type;

    private Object source;

    public EsSourceData(String type, Object source) {
        this.type = type;
        this.source = source;
        this.timestamp = System.currentTimeMillis();;
    }
}
