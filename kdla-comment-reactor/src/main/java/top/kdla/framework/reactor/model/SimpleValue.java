package top.kdla.framework.reactor.model;

import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Type;

@AllArgsConstructor(staticName = "of")
class SimpleValue implements Value, Serializable {

    private final Object nativeValue;

    @Override
    public Object get() {
        return nativeValue;
    }

}
