package top.kdla.framework.supplement.trdcloud.timer.manager.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.kdla.framework.supplement.trdcloud.timer.XxlJobUrlConstants;
import top.kdla.framework.supplement.trdcloud.timer.config.XxlJobConfig;
import top.kdla.framework.supplement.trdcloud.timer.manager.TimeJobManagerService;
import top.kdla.framework.supplement.trdcloud.timer.manager.model.XxlJobInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "xxl.enabled", name = "xxl", havingValue = "true")
public class EnnIotXxlJobManager implements TimeJobManagerService {

    private static final ConcurrentHashMap<String, String> TIME_TASK_MAP = new ConcurrentHashMap<>();
    @Autowired
    XxlJobConfig xxlJobConfig;

    @Override
    public String register(String name,String cron, String handler, String param) {
        String taskId = this.addJob(name,cron,handler,param);
        this.startJob(taskId);
        return taskId;
    }

    @Override
    public Boolean unRegister(String id) {
        this.stopJob(id);
        this.removeJob(id);
        return true;
    }

    public String addJob(String name,String cron, String handler, String param) {
//
        XxlJobInfo xxlJobInfo = new XxlJobInfo(cron, handler, param).build();
        xxlJobInfo.setJobDesc(name);
        String jsonStr = JSONUtil.toJsonStr(xxlJobInfo);

        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.ADD_JOB;
        String body = HttpUtil.createPost(url).form(JSONUtil.toBean(jsonStr, Map.class)).execute().body();
        JSONObject jsonObject = JSONUtil.parseObj(body);
        if (jsonObject.getInt("code") == 200) {
            String taskId = jsonObject.getStr("content");
            TIME_TASK_MAP.put(taskId, jsonStr);
            return taskId;
        } else {
            log.error("job 添加异常，info：{}", body);
            return null;
        }
    }

    @Override
    public Boolean removeJob(String id) {

        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.REMOVE_JOB;
        String body = HttpUtil.createPost(url).form("id", Integer.valueOf(id)).execute().body();
        JSONObject jsonObject = JSONUtil.parseObj(body);
        if (jsonObject.getInt("code") == 200) {
            TIME_TASK_MAP.remove(id);
            return Boolean.TRUE;
        }
        log.warn("job 删除异常：{}", body);
        return Boolean.FALSE;
    }

    @Override
    public Boolean startJob(String id) {
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.START_JOB;
        String body = HttpUtil.createPost(url).form("id", Integer.valueOf(id)).execute().body();
        JSONObject jsonObject = JSONUtil.parseObj(body);
        if (jsonObject.getInt("code") == 200) {
            return Boolean.TRUE;
        }
        log.warn("job 删除异常：{}", body);
        return Boolean.FALSE;
    }

    @Override
    public Boolean stopJob(String id) {
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.STOP_JOB;
        String body = HttpUtil.createPost(url).form("id", Integer.valueOf(id)).execute().body();
        JSONObject jsonObject = JSONUtil.parseObj(body);
        if (jsonObject.getInt("code") == 200) {
            return Boolean.TRUE;
        }
        log.warn("job 删除异常：{}", body);
        return Boolean.FALSE;
    }

    @Override
    public Map<String, Object> pageList(int start, int length, int triggerStatus) {
        String url = xxlJobConfig.getAdminAddresses() + XxlJobUrlConstants.PAGE_LIST_JOB;
        Map<String, Object> stringObjectMap = new HashMap<>();
        stringObjectMap.put("jobGroup", 2);
        stringObjectMap.put("start", start);
        stringObjectMap.put("length", length);
        stringObjectMap.put("triggerStatus", triggerStatus);
        String body = HttpUtil.createPost(url).form("jobGroup", 2, "start", start, "length", length, "triggerStatus", triggerStatus).execute().body();

        if (StringUtils.isNotBlank(body)) {
            return com.alibaba.fastjson.JSONObject.parseObject(body);
        } else {
            return new HashMap<>();
        }
    }


    public static void main(String[] args) {
        XxlJobConfig xxlJobConfig1 = new XxlJobConfig();
        xxlJobConfig1.setAdminAddresses("http://localhost:8080/xxl-job-admin");
        EnnIotXxlJobManager ennIotXxlJobManager = new EnnIotXxlJobManager();
        ennIotXxlJobManager.xxlJobConfig = xxlJobConfig1;

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
