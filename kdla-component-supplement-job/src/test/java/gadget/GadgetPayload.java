package gadget;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/** 白名单外的 Serializable，readObject 置标志位以证明未被实例化。 */
public class GadgetPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    public static volatile boolean deserialized;

    private String value = "pwned";

    public String getValue() {
        return value;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        deserialized = true;
    }
}
