package gadget;

/** 非 Serializable POJO，走 JSON 分支；用于验证 loadClass 包前缀白名单。 */
public class JsonPojo {
    private String name;

    public JsonPojo() {
    }

    public JsonPojo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
