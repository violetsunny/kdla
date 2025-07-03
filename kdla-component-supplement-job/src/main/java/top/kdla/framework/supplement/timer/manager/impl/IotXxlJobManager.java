package top.kdla.framework.supplement.timer.manager.impl;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.http.HttpMethod;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.kdla.framework.supplement.http.VertxHttpClient;
import top.kdla.framework.supplement.timer.XxlJobUrlConstants;
import top.kdla.framework.supplement.timer.config.XxlJobConfig;
import top.kdla.framework.supplement.timer.manager.TimeJobManagerService;
import top.kdla.framework.supplement.timer.manager.model.XxlJobInfo;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "xxl.enabled", name = "xxl", havingValue = "true")
public class IotXxlJobManager implements TimeJobManagerService {

    //private static final ConcurrentHashMap<String, String> TIME_TASK_MAP = new ConcurrentHashMap<>();
    @Autowired
    private XxlJobConfig xxlJobConfig;
    @Resource
    private VertxHttpClient vertxHttpClient;

    @Override
    public String register(String name, String cron, String handler, String param) {
        String taskId = this.addJob(name, cron, handler, param);
        this.startJob(taskId);
        return taskId;
    }

    @Override
    public Boolean unRegister(String id) {
        this.stopJob(id);
        this.removeJob(id);
        return true;
    }

    @SneakyThrows
    public String addJob(String name, String cron, String handler, String param) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo(cron, handler, param).build();
        xxlJobInfo.setJobDesc(name);
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.ADD_JOB;

        //String body = HttpUtil.createPost(url).form(JSONUtil.toBean(jsonStr, Map.class)).execute().body();
        CompletableFuture<JSONObject> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, url, null, xxlJobInfo, JSONObject.class);
        JSONObject jsonObject = futureRes.get();
        if (jsonObject.getIntValue("code") == 200) {
            return jsonObject.getString("content");
        } else {
            log.error("job 添加异常，info：{}", jsonObject.toJSONString());
            return null;
        }
    }

    @Override
    @SneakyThrows
    public Boolean removeJob(String id) {
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.REMOVE_JOB;
        Map<String, Object> stringObjectMap = new HashMap<>();
        stringObjectMap.put("id", Integer.valueOf(id));
        CompletableFuture<JSONObject> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, url, null, stringObjectMap, JSONObject.class);
        JSONObject jsonObject = futureRes.get();
        //String body = HttpUtil.createPost(url).form("id", Integer.valueOf(id)).execute().body();
        if (jsonObject.getIntValue("code") == 200) {
            return Boolean.TRUE;
        } else {
            log.error("job 删除异常，info：{}", jsonObject.toJSONString());
            return Boolean.FALSE;
        }
    }

    @Override
    @SneakyThrows
    public Boolean startJob(String id) {
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.START_JOB;
        Map<String, Object> stringObjectMap = new HashMap<>();
        stringObjectMap.put("id", Integer.valueOf(id));
        CompletableFuture<JSONObject> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, url, null, stringObjectMap, JSONObject.class);
        JSONObject jsonObject = futureRes.get();
        //String body = HttpUtil.createPost(url).form("id", Integer.valueOf(id)).execute().body();
        if (jsonObject.getIntValue("code") == 200) {
            return Boolean.TRUE;
        } else {
            log.error("job 开始异常，info：{}", jsonObject.toJSONString());
            return Boolean.FALSE;
        }
    }

    @Override
    @SneakyThrows
    public Boolean stopJob(String id) {
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.STOP_JOB;
        Map<String, Object> stringObjectMap = new HashMap<>();
        stringObjectMap.put("id", Integer.valueOf(id));
        CompletableFuture<JSONObject> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, url, null, stringObjectMap, JSONObject.class);
        JSONObject jsonObject = futureRes.get();
        //String body = HttpUtil.createPost(url).form("id", Integer.valueOf(id)).execute().body();
        if (jsonObject.getIntValue("code") == 200) {
            return Boolean.TRUE;
        } else {
            log.error("job 停止异常，info：{}", jsonObject.toJSONString());
            return Boolean.FALSE;
        }
    }

    @Override
    @SneakyThrows
    public Map<String, Object> pageList(int start, int length, int triggerStatus) {
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.PAGE_LIST_JOB;
        Map<String, Object> stringObjectMap = new HashMap<>();
        stringObjectMap.put("jobGroup", 2);
        stringObjectMap.put("start", start);
        stringObjectMap.put("length", length);
        stringObjectMap.put("triggerStatus", triggerStatus);
        CompletableFuture<JSONObject> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, url, null, stringObjectMap, JSONObject.class);
        return futureRes.get();
        //String body = HttpUtil.createPost(url).form("jobGroup", 2, "start", start, "length", length, "triggerStatus", triggerStatus).execute().body();
    }


    public static void main(String[] args) {
        XxlJobConfig xxlJobConfig1 = new XxlJobConfig();
        xxlJobConfig1.setAdminAddresses("http://localhost:8080/xxl-job-admin");
        IotXxlJobManager iotXxlJobManager = new IotXxlJobManager();
        iotXxlJobManager.xxlJobConfig = xxlJobConfig1;

        String handler = "GatewayXxlJob";
        String cron = "0 0 0/1 * * ?";
        String param = " code params";

//        Integer newId = ennIotXxlJobManager.addJob(cron, handler, param);
//        System.out.println(s);

//        String s1 = ennIotXxlJobManager.startJob(7);
//        System.out.println(s1);

//        String s2 = ennIotXxlJobManager.stopJob(7);
//        System.out.println(s2);

//        String s3 = String.valueOf(ennIotXxlJobManager.removeJob(String.valueOf(7)));
//        System.out.println(s3);

//        ennIotXxlJobManager.pageList();


    }
}
