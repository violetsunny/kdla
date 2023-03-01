package top.kdla.framework.domain.shared;

import java.io.Serializable;

/**
 * Entity interface
 *
 * @author kll
 * @since 2021/7/9 14:15
 **/
public interface Entity<T> extends Serializable {

    /**
     * Entities compare by identity, not by attributes.
     *
     * @param other The other entity.
     * @return true if the identities are the same, regardless of other attributes.
     */
    boolean sameIdentityAs(T other);
}
