package top.kdla.framework.supplement.trdcloud.bo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;

import java.util.Map;

@Data
public class TrdPlatformInfoBo extends TrdPlatformInfoEntity {

    public Map<String, String> getConfigMap() {
        if(StringUtils.isBlank(getConfigJson())){
            return null;
        }
        return JSONObject.parseObject(getConfigJson(), new TypeReference<Map<String, String>>(){});
    }
}
