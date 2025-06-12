package top.kdla.framework.supplement.trdcloud.timer.manager.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.kdla.framework.supplement.job.handler.IJobHandler;
import top.kdla.framework.supplement.trdcloud.timer.annotation.EnnIotXxlJob;
import top.kdla.framework.supplement.trdcloud.timer.handler.EnnIotXxlJobHandler;
import top.kdla.framework.supplement.trdcloud.timer.manager.TimeJobManagerService;
import top.kdla.framework.supplement.trdcloud.timer.manager.model.XxlJobInfo;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Slf4j
@Component
@ConditionalOnProperty(prefix = "xxl.enabled", name = "local", havingValue = "true")
public class LocalJobManager implements TimeJobManagerService, ApplicationContextAware, SmartInitializingSingleton {

    private static final ConcurrentHashMap<String, String> TIME_TASK_MAP = new ConcurrentHashMap<>();
    private static final String TIME_TASK_CONSTANT = "ENN_IOT_LOCAL_JOB";

    private static Map<String, EnnIotXxlJobHandler> SERVICE_BEAN_MAP = new HashMap<>();
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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

    @Override
    public String addJob(String name,String cron, String handler, String param) {

        XxlJobInfo xxlJobInfo = new XxlJobInfo(cron, handler, param).build();
        xxlJobInfo.setJobDesc(name);
        String jsonStr = JSONUtil.toJsonStr(xxlJobInfo);
        String uuid = IdUtil.fastSimpleUUID();

        redisTemplate.opsForHash().put(TIME_TASK_CONSTANT, uuid, jsonStr);
        return uuid;


    }

    @Override
    public Boolean removeJob(String taskId) {

        Boolean aBoolean = removeTask(taskId);
        redisTemplate.opsForHash().delete(TIME_TASK_CONSTANT, taskId);
        return aBoolean;
    }

    @Override
    public Boolean startJob(String taskId) {

        Object value = redisTemplate.opsForHash().get(TIME_TASK_CONSTANT, taskId);
        if(value == null){
            return false;
        }

        XxlJobInfo xxlJobInfo = JSONUtil.toBean(String.valueOf(value), XxlJobInfo.class);
        xxlJobInfo.setTriggerStatus(1);

        EnnIotXxlJobHandler iotXxlJobHandler = null;
        try {
            if (SERVICE_BEAN_MAP.containsKey(xxlJobInfo.getExecutorHandler())) {
                iotXxlJobHandler = SERVICE_BEAN_MAP.get(xxlJobInfo.getExecutorHandler());
            } else {
                log.warn(" handler 不存在：{}", xxlJobInfo.getExecutorHandler());
                return false;
            }
        } catch (Exception e) {
            log.warn("本地执行 handler 不存在：{}", e.getMessage());
            return false;
        }

        EnnIotXxlJobHandler finalIotXxlJobHandler = iotXxlJobHandler;
        String addTask = addTask(taskId, xxlJobInfo.getScheduleConf(), xxlJobInfo.getExecutorParam(), s -> {
            try {
                finalIotXxlJobHandler.doExecute(s);
            } catch (Exception e) {
                log.warn("本地执行器执行异常", e);
            }
        });
        redisTemplate.opsForHash().put(TIME_TASK_CONSTANT, taskId, JSONUtil.toJsonStr(xxlJobInfo));
        return StringUtils.isNotBlank(addTask);
    }

    @Override
    public Boolean stopJob(String id) {

        Boolean aBoolean = removeTask(id);

        Object value = redisTemplate.opsForHash().get(TIME_TASK_CONSTANT, id);
        XxlJobInfo xxlJobInfo = JSONUtil.toBean(String.valueOf(value), XxlJobInfo.class);
        xxlJobInfo.setTriggerStatus(1);
        redisTemplate.opsForHash().put(TIME_TASK_CONSTANT, id, JSONUtil.toJsonStr(xxlJobInfo));

        return aBoolean;
    }

    @Override
    public Map<String, Object> pageList(int start, int length, int triggerStatus) {
        Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(TIME_TASK_CONSTANT);
        HashMap<String, Object> result = new HashMap<>();
        int runSize = TIME_TASK_MAP.size();

        List<XxlJobInfo> xxlJobInfos = new ArrayList<>();
        if (Objects.isNull(hashEntries)) {
            result.put("data", xxlJobInfos);
            result.put("recordsTotal", 0);
            result.put("recordsFiltered", 0);
            return result;
        }

        hashEntries.forEach((key, value1) -> {

            String value = String.valueOf(value1);
            XxlJobInfo xxlJobInfo = JSONUtil.toBean(value, XxlJobInfo.class);
            xxlJobInfo.setChildJobId(key.toString());
            if (TIME_TASK_MAP.containsKey(key.toString())) {
                xxlJobInfo.setTriggerStatus(1);
            } else {
                xxlJobInfo.setTriggerStatus(0);
            }
            xxlJobInfos.add(xxlJobInfo);
        });

        int totalSize = hashEntries.size();
        int recordsFiltered;
        List<XxlJobInfo> collect = null;
        if (triggerStatus == 1 || triggerStatus == 0) {
            collect = xxlJobInfos.stream().filter(xxlJobInfo -> xxlJobInfo.getTriggerStatus() == triggerStatus).collect(Collectors.toList());
        } else {
            recordsFiltered = totalSize;
            collect = xxlJobInfos;

        }
        result.put("data", collect);
        result.put("recordsTotal", totalSize);
        result.put("recordsFiltered", collect.size());
        return result;
    }


    /**
     * 添加定时任务
     *
     * @param taskId
     * @param cornPattern
     * @param input
     * @param consumer
     * @return
     */
    public static String addTask(String taskId, String cornPattern, String input, Consumer<String> consumer) {

        String schedule = CronUtil.schedule(taskId, cornPattern, (Task) () -> ThreadUtil.execute(() -> consumer.accept(input)));
        TIME_TASK_MAP.put(schedule, String.valueOf(new Date()));
        return schedule;
    }


    /**
     * 删除定时任务
     *
     * @param taskId
     * @return
     */
    public static Boolean removeTask(String taskId) {
        if (!TIME_TASK_MAP.containsKey(taskId)) {
            return true;
        }
        boolean remove = CronUtil.remove(taskId);
        if (remove) {
            TIME_TASK_MAP.remove(taskId);
        }
        return remove;
    }


    /**
     * 更新定时任务corn
     *
     * @param taskId
     * @param cornPattern
     * @return
     */
    public static Boolean updateTask(String taskId, String cornPattern) {
        CronUtil.updatePattern(taskId, CronPattern.of(cornPattern));
        return true;
    }

    /**
     * 打印定时任务列表
     */
    public static void taskList() {
        TIME_TASK_MAP.forEach((taskId, time) -> {
            log.info("当前定时执行任务:{},start time :{}", taskId, time);
        });
    }


    @SneakyThrows
    public static void main(String[] args) {

//        CronUtil.setMatchSecond(true);
//        CronUtil.start();

//        Consumer<String> consumer = s -> {
//
//            log.info("任务执行" + s);
//            Date date1 = new Date();
//            for (int i = 0; i < 1; i++) {
//                Date date = CronPatternUtil.nextDateAfter(CronPattern.of("50 50 * * * ? "), date1, true);
//                log.debug(date1 + "-" + date);
//                date1 = new Date(date.getTime() + 1000);
//            }
//        };
//
//        List<Date> dates = CronPatternUtil.matchedDates("0/10 * * * * ?", new Date(), 3, true);
//
//        String test0 = addTask("1", "0/10 * * * * ?", "test0", consumer);


//        LocalJobManager localJobManager = new LocalJobManager();
//        String s = localJobManager.addJob("0/10 * * * * ?", "cn.enncloud.iot.gateway.timer.job.GatewayXxlJob", "test");
//        localJobManager.startJob(s);
//        Thread.sleep(20000);
//
//
//        taskList();


    }

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        CronUtil.setMatchSecond(true);
        CronUtil.start();
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(EnnIotXxlJob.class);
        if (serviceBeanMap.size() > 0) {

            for (Object serviceBean : serviceBeanMap.values()) {
                if (serviceBean instanceof IJobHandler) {
                    String name = ((EnnIotXxlJob) serviceBean.getClass().getAnnotation(EnnIotXxlJob.class)).value();
                    EnnIotXxlJobHandler handler = (EnnIotXxlJobHandler) serviceBean;
                    if (SERVICE_BEAN_MAP.containsKey(name)) {
                        throw new RuntimeException(String.format("LocalIotXxlJob jobhandler(%s) naming conflicts.", name));
                    }

                    SERVICE_BEAN_MAP.put(name, handler);
                }
            }
        }

        Map<Object, Object> keys = redisTemplate.opsForHash().entries(TIME_TASK_CONSTANT);
        if (MapUtils.isNotEmpty(keys)) {
            keys.forEach((k, v) -> {

                XxlJobInfo xxlJobInfo = JSONUtil.toBean(v.toString(), XxlJobInfo.class);
                if (xxlJobInfo.getTriggerStatus() == 1) {
                    startJob((String) k);
                }
            });
        }
    }
}
