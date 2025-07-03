/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.supplement.trdcloud.server;

import cn.hutool.cron.pattern.parser.PatternParser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.kdla.framework.domain.ApplicationContextHelp;
import top.kdla.framework.dto.PageResponse;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.http.VertxHttpClient;
import top.kdla.framework.supplement.trdcloud.TrdPlatformCloudServer;
import top.kdla.framework.supplement.trdcloud.bo.*;
import top.kdla.framework.supplement.trdcloud.cloud.*;
import top.kdla.framework.supplement.trdcloud.enums.TrdPlatformEnum;
import top.kdla.framework.supplement.trdcloud.repository.*;
import top.kdla.framework.supplement.timer.manager.TimeJobManagerService;
import top.kdla.framework.supplement.timer.manager.impl.IotXxlJobManager;
import top.kdla.framework.supplement.timer.manager.impl.LocalJobManager;
import top.kdla.framework.supplement.trdcloud.utils.StringUtil;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author kanglele
 * @version $Id: TrdPlatformCloudServerImpl, v 0.1 2024/3/13 16:44 kanglele Exp $
 */
@Service
@Slf4j
public class TrdPlatformCloudServerImpl implements TrdPlatformCloudServer {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private VertxHttpClient vertxHttpClient;
    @Resource
    private TrdPlatformTaskRepository trdPlatformTaskRepository;
    @Resource
    private TrdPlatformInfoRepository trdPlatformInfoRepository;
    @Resource
    private TrdPlatformApiRepository trdPlatformApiRepository;
    @Resource
    private TrdPlatformApiParamRepository trdPlatformApiParamRepository;
    @Resource
    private TrdPlatformModelRefRepository trdPlatformModelRefRepository;
    @Resource
    private TrdPlatformMeasureRefRepository trdPlatformMeasureRefRepository;
    @Resource
    private TrdPlatformConverter trdPlatformConverter;

    @Value("${kdla.trdcloud.switch.reslog:false}")
    private Boolean reslog;
    @Value("${kdla.trdcloud.switch.datasize:100}")
    private Integer dataSize;
    @Value("${kdla.trdcloud.job:local}")
    private String jobType;

    private static final String TOKEN_PRE = "TrdPlatformCloudAuth:";
    private static final String TASK_JOB = "TrdPlatformCloudTask:";

    private static final String PAGE_PLACEHOLDER = "#page#";

    @Override
    public JSONObject sendRequest(String method, String url, Map<String, String> headers, Object req, Class res) throws Exception {
        log.info("CloudDockingReqManage-send  url:{}  headers:{}  req:{}", url, JSON.toJSONString(headers), JSON.toJSONString(req));

        JSONObject jsonObject;
        try {
            CompletableFuture<HttpResponse<Buffer>> future = vertxHttpClient.sendRequest(method, url, headers, req);
            HttpResponse<Buffer> response = future.get();
            if (res.equals(String.class)) {
                String result = response.bodyAsString();
                if (result.startsWith("{") && result.endsWith("}")) {
                    jsonObject = JSONObject.parseObject(result);
                } else {
                    jsonObject = new JSONObject();
                    jsonObject.put("data", result);
                }
            } else {
                Object result = response.bodyAsJson(res);//默认json返回
                jsonObject = JSONObject.parseObject(JSONObject.toJSONString(result));
            }

            if (response.headers() != null) {
                JSONObject headerRes = JSONArray.parseArray(JSON.toJSONString(response.headers().entries())).stream()
                        .map(o -> (JSONObject) o)
                        .flatMap(json -> json.keySet().stream()
                                .collect(Collectors.toMap(Function.identity(), json::get, (value1, value2) -> value2)).entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (value1, value2) -> value2, JSONObject::new));
                jsonObject.put("headers", headerRes);//合并对象
            }

            if (reslog) {
                log.info("CloudDockingReqManage-res {} {}", url, JSON.toJSONString(jsonObject));
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.FAIL.getCode(), "调用接口异常::%s", ExceptionUtils.getMessage(e));
        }

        return jsonObject;
    }

    @Override
    public List<TrdPlatformTask> taskWorkList(String pCode) {
        List<TrdPlatformTaskBo> bos = trdPlatformTaskRepository.queryByCode(pCode);
        return trdPlatformConverter.toTrdPlatformTasks(bos);
    }

    @Override
    public TrdPlatformTask taskWork(String pCode, String taskCode) {
        TrdPlatformTaskBo bo = trdPlatformTaskRepository.searchByCode(pCode, null, taskCode);
        return trdPlatformConverter.toTrdPlatformTask(bo);
    }

    @Override
    public TrdPlatformReq downReqContext(String productId, String taskCode) throws Exception {
        TrdPlatformTaskBo bo = trdPlatformTaskRepository.searchByCode(null, productId, taskCode);
        if (bo == null) {
            return null;
        }
        return reqContext(bo);
    }

    @Override
    public TrdPlatformReq taskReqContext(String pCode, String taskCode) throws Exception {
        TrdPlatformTaskBo bo = trdPlatformTaskRepository.searchByCode(pCode, null, taskCode);
        if (bo == null) {
            return null;
        }
        return reqContext(bo);
    }

    public TrdPlatformReq reqContext(TrdPlatformTaskBo taskBo) throws Exception {
        TrdPlatformInfoBo infoBo = trdPlatformInfoRepository.queryByCode(taskBo.getPlatformCode());
        if (infoBo == null) {
            return null;
        }
        TrdPlatformApiBo apiBo = trdPlatformApiRepository.getById(taskBo.getApiId());
        if (apiBo == null) {
            return null;
        }
        List<TrdPlatformReqTask> reqTasks = new ArrayList<>();
        TrdPlatformReqTask reqTask = new TrdPlatformReqTask();
        reqTask.setCode(taskBo.getTaskCode());
        reqTask.setLimit(apiBo.getCallLimit());

        String url = "";
        if (StringUtils.isNotBlank(apiBo.getFullUrl())) {
            url = String.format("%s%s", infoBo.getConfigMap().get("baseUrl"), apiBo.getFullUrl());
        }

        TrdPlatformAuthToken authToken = null;
        if (apiBo.getAuthType() == TrdPlatformEnum.AuthWayEnum.TOKEN.getCode()) {
            authToken = this.authToken(taskBo.getPlatformCode(), apiBo.getAuthApi());
        }

        List<TrdPlatformApiParamBo> paramRes = null;
        if (apiBo.getHasParam() == 1) {
            paramRes = trdPlatformApiParamRepository.getById(taskBo.getApiId());
        }

        //TODO 特殊占位符替换
        List<List<TrdPlatformApiParam>> paramBoList = transformSpecial(paramRes, taskBo.getProductId());

        List<TrdPlatformBody> bodies = new ArrayList<>();

        if (CollectionUtils.isEmpty(paramBoList)) {
            bodies.addAll(createReqBody(apiBo, authToken, null, url));
        } else {
            for (List<TrdPlatformApiParam> paramBosTwo : paramBoList) {
                bodies.addAll(createReqBody(apiBo, authToken, paramBosTwo, url));
            }
        }

        reqTask.setApiId(taskBo.getApiId());
        reqTask.setAuthApi(apiBo.getAuthApi());
        reqTask.setApiType(apiBo.getApiType());
        reqTask.setBodies(bodies);
        reqTasks.add(reqTask);
        return TrdPlatformReq.builder()
                .platformCode(taskBo.getPlatformCode())
                .productId(taskBo.getProductId())
                .reqChildren(reqTasks)
                .build();
    }

    private List<List<TrdPlatformApiParam>> transformSpecial(List<TrdPlatformApiParamBo> paramRes, String productId) throws Exception {
        List<List<TrdPlatformApiParam>> paramBoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(paramRes)) {
            List<TrdPlatformApiParam> paramBos = paramRes.stream().map(TrdPlatformCloudServerImpl::transformApiParam).collect(Collectors.toList());
            //TODO 可以执行groovyShellCal
            //对body的请求做再次加工
            Map<String, Object> finalBodyre = new LinkedHashMap<>();
            finalBodyre.put("productId", productId);
            finalBodyre.put("trdPlatformCloudServer", this);
            paramBos = paramBos.stream().sorted(Comparator.comparing(TrdPlatformApiParam::getId)) //排序问题
                    .peek(param -> {
                        if (param.getParamType() == TrdPlatformEnum.ParamTypeEnum.GROOVY.getCode()) {
                            Object obj = groovyShellCal(String.valueOf(param.getParamValue()), finalBodyre);
                            param.setParamValue(obj);
                            finalBodyre.put(param.getParamKey(), obj);
                        } else {
                            finalBodyre.put(param.getParamKey(), param.getParamValue());
                        }
                    }).collect(Collectors.toList());
            paramBoList.add(paramBos);
        }

        return paramBoList;
    }

    private List<TrdPlatformBody> createReqBody(TrdPlatformApiBo apiBo, TrdPlatformAuthToken authToken, List<TrdPlatformApiParam> paramBosTwo, String url) throws Exception {
        List<TrdPlatformBody> bodies = new ArrayList<>();
        if (apiBo.getHasPages() == 1) {
            //参数中填写key和值，将key再存入标志中
            Integer page = apiBo.getPageStartNo();
            Integer pageSize = apiBo.getPageSize();
            String pageNumberKey = apiBo.getPageNumberKey();
            String pageSizeKey = apiBo.getPageSizeKey();
            Integer pagePosition = apiBo.getPagePosition();

            Integer finalPage = page;
            HashMap<String, Object> pageMap = new HashMap<String, Object>() {{
                put(pageNumberKey, finalPage);
                put(pageSizeKey, pageSize);
            }};

            Boolean pageFlag = false;
            if (CollectionUtils.isNotEmpty(paramBosTwo)) {
                pageFlag = paramBosTwo.stream().anyMatch(t -> String.valueOf(t.getParamValue()).equalsIgnoreCase(PAGE_PLACEHOLDER));
            }

            int count = 0;
            if (apiBo.getTotalNumberType() == TrdPlatformEnum.TotalDataGetWayEnum.FIXED.getCode()) {
                count = Integer.parseInt(apiBo.getTotalNumberConfig());
            }
            if (apiBo.getTotalNumberType() == TrdPlatformEnum.TotalDataGetWayEnum.ORIGINAL_API.getCode()) {

                TrdPlatformBody bodyre = createPageBody(authToken, paramBosTwo, url, apiBo.getMethod(), apiBo, pageMap, pagePosition, pageFlag);

                JSONObject obj = this.sendRequest(bodyre.getMethod(), bodyre.getUrl(), bodyre.getHeader(), bodyre.getBody(), JSONObject.class);
                if (apiBo.getBodyAnalysisType() == TrdPlatformEnum.BodyParsingMethodEnum.JSON.getCode()) {
                    Object v = JSONPath.read(obj.toJSONString(), apiBo.getBodyAnalysisCode());
                    count = Integer.parseInt(String.valueOf(v));
                }
                if (apiBo.getBodyAnalysisType() == TrdPlatformEnum.BodyParsingMethodEnum.GROOVY.getCode()) {
                    Object auth = groovyShellCal(apiBo.getBodyAnalysisCode(), obj);
                    count = Integer.parseInt(String.valueOf(auth));
                }
            }
            if (apiBo.getTotalNumberType() == TrdPlatformEnum.TotalDataGetWayEnum.NEW_API.getCode()) {
                Long countApi = Long.parseLong(apiBo.getTotalNumberConfig());
                TrdPlatformBody countbody = this.createApi(countApi);
                if (countbody != null) {
                    JSONObject countobj = this.sendRequest(countbody.getMethod(), countbody.getUrl(), countbody.getHeader(), countbody.getBody(), String.class);
                    log.info("获取count:{}", countobj);
                    if (countbody.getBodyAnalysisType() == TrdPlatformEnum.BodyParsingMethodEnum.JSON.getCode()) {
                        Object v = JSONPath.read(countobj.toJSONString(), countbody.getBodyAnalysisCode());
                        count = Integer.parseInt(String.valueOf(v));
                    } else if (countbody.getBodyAnalysisType() == TrdPlatformEnum.BodyParsingMethodEnum.GROOVY.getCode()) {
                        Object auth = groovyShellCal(countbody.getBodyAnalysisCode(), countobj);
                        count = Integer.parseInt(String.valueOf(auth));
                    } else {
                        count = Integer.parseInt(countobj.getString("data"));
                    }
                }

            }
            PageResponse pageResponse = PageResponse.of(null, count, pageSize, page);

            for (int i = 1; i <= pageResponse.getTotalPages(); i++) {
                pageMap.put(pageNumberKey, String.valueOf(page));
                bodies.add(createPageBody(authToken, paramBosTwo, url, apiBo.getMethod(), apiBo, pageMap, pagePosition, pageFlag));
                ++page;
            }

        } else {
            bodies.add(createBody(authToken, paramBosTwo, url, apiBo.getMethod(), apiBo));
        }

        return bodies;
    }

    private TrdPlatformBody createApi(Long api) throws Exception {
        TrdPlatformApiBo apiBo = trdPlatformApiRepository.getById(api);
        if (apiBo == null) {
            return null;
        }
        TrdPlatformInfoBo infoBo = trdPlatformInfoRepository.queryByCode(apiBo.getPlatformCode());
        if (infoBo == null) {
            return null;
        }
        TrdPlatformAuthToken authToken = null;
        if (apiBo.getAuthType() == TrdPlatformEnum.AuthWayEnum.TOKEN.getCode()) {
            authToken = this.authToken(apiBo.getPlatformCode(), apiBo.getAuthApi());
        }
        List<TrdPlatformApiParam> params = null;
        List<TrdPlatformApiParamBo> paramRes = null;
        if (apiBo.getHasParam() == 1) {
            paramRes = trdPlatformApiParamRepository.getById(api);
            if (CollectionUtils.isNotEmpty(paramRes)) {
                params = paramRes.stream().map(TrdPlatformCloudServerImpl::transformApiParam).collect(Collectors.toList());
            }
        }
        String url = "";
        if (StringUtils.isNotBlank(apiBo.getFullUrl())) {
            url = String.format("%s%s", infoBo.getConfigMap().get("baseUrl"), apiBo.getFullUrl());
        }
        return createBody(authToken, params, url, apiBo.getMethod(), apiBo);
    }

    public synchronized TrdPlatformAuthToken authToken(String pCode, Long authApi) throws Exception {
        //从缓存获取token
        String key = String.format("%s%s_%s", TOKEN_PRE, pCode, authApi);
        Object tokenObject = redisTemplate.opsForValue().get(key);
        TrdPlatformAuthToken authToken = null;
        if (Objects.nonNull(tokenObject)) {
            try {
                if (tokenObject instanceof String) {
                    authToken = JSONObject.parseObject((String) tokenObject, TrdPlatformAuthToken.class);
                } else {
                    authToken = JSONObject.parseObject(JSONObject.toJSONString(tokenObject), TrdPlatformAuthToken.class);
                }
                if (authToken != null) {
                    //在redis中直接返回
                    return authToken;
                }
            } catch (Exception e) {
                log.error("转换tokenBo error", e);
            }
        }
        TrdPlatformBody body = this.createApi(authApi);
        return this.authRefreshToken(pCode, authApi, body);
    }

    @Override
    public TrdPlatformAuthToken authRefreshToken(String pCode, Long authApi, TrdPlatformBody body) throws Exception {
        //从缓存获取token
        String key = String.format("%s%s_%s", TOKEN_PRE, pCode, authApi);
        TrdPlatformAuthToken authToken = createAuthToken(body);
        if (authToken == null) {
            return null;
        }
        //存入Redis-时间都要改成秒
        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(authToken), Duration.ofSeconds(Long.parseLong(authToken.getExpirationTime())));
        return authToken;
    }

    private TrdPlatformAuthToken createAuthToken(TrdPlatformBody body) throws Exception {
        if (body == null) {
            return null;
        }
        JSONObject obj = this.sendRequest(body.getMethod(), body.getUrl(), body.getHeader(), body.getBody(), JSONObject.class);
        if (obj == null) {
            return null;
        }
        if (body.getBodyAnalysisType() == TrdPlatformEnum.BodyParsingMethodEnum.JSON.getCode()) {
            TrdPlatformAuthToken authToken = JSONObject.parseObject(body.getBodyAnalysisCode(), TrdPlatformAuthToken.class);
            Object v = JSONPath.read(obj.toJSONString(), authToken.getParamValue());
            authToken.setParamValue(String.valueOf(v));

            if (authToken.getExpirationTime().startsWith("$")) {
                try {
                    Object t = JSONPath.read(obj.toJSONString(), authToken.getExpirationTime());
                    if (t != null) {
                        authToken.setExpirationTime(String.valueOf(t));
                    }
                } catch (Exception e) {
                    log.warn("Exception", e);
                }
            }

            authToken.setCreateTime(System.currentTimeMillis());
            return authToken;
        }
        if (body.getBodyAnalysisType() == TrdPlatformEnum.BodyParsingMethodEnum.GROOVY.getCode()) {
            Object auth = groovyShellCal(body.getBodyAnalysisCode(), obj);
            TrdPlatformAuthToken authToken = JSONObject.parseObject(JSONObject.toJSONString(auth), TrdPlatformAuthToken.class);
            authToken.setCreateTime(System.currentTimeMillis());
            return authToken;
        }

        return null;
    }

    private TrdPlatformBody createPageBody(TrdPlatformAuthToken authToken, List<TrdPlatformApiParam> paramBos, String url, String method, TrdPlatformApiBo apiBo, HashMap<String, Object> pageMap, Integer pagePosition, Boolean pageFlag) throws Exception {
        List<TrdPlatformApiParam> paramBosTwo = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(paramBos)) {
            for (TrdPlatformApiParam paramBo : paramBos) {
                TrdPlatformApiParam paramBoTwo = new TrdPlatformApiParam();
                BeanUtils.copyProperties(paramBoTwo, paramBo);
                if (String.valueOf(paramBoTwo.getParamValue()).equalsIgnoreCase(PAGE_PLACEHOLDER)) {
                    paramBoTwo.setParamValue(JSON.toJSONString(pageMap));
                }
                paramBosTwo.add(paramBoTwo);
            }
        }

        if (!pageFlag) {
            for (Map.Entry<String, Object> entry : pageMap.entrySet()) {
                TrdPlatformApiParam paramBo = new TrdPlatformApiParam();
                paramBo.setId(0L);
                paramBo.setParamKey(entry.getKey());
                paramBo.setParamType(TrdPlatformEnum.ParamTypeEnum.FIXED.getCode());
                paramBo.setParamPosition(pagePosition);
                paramBo.setParamValue(entry.getValue());
                paramBosTwo.add(paramBo);
            }
        }

        return createBody(authToken, paramBosTwo, url, method, apiBo);
    }

    private static TrdPlatformApiParam transformApiParam(TrdPlatformApiParamBo paramDto) {
        TrdPlatformApiParam paramBoTwo = new TrdPlatformApiParam();
        try {
            BeanUtils.copyProperties(paramBoTwo, paramDto);
        } catch (Exception e) {
            log.error("BeanUtils异常", e);
        }
        return paramBoTwo;
    }

    private TrdPlatformBody createBody(TrdPlatformAuthToken authToken, List<TrdPlatformApiParam> paramBos, String url, String method, TrdPlatformApiBo apiBo) {
        Map<String, Object> authMap = new LinkedHashMap<>();
        if (authToken != null) {
            authMap.put(authToken.getParamKey(), authToken.getParamValue());
        }

        if (CollectionUtils.isEmpty(paramBos)) {
            url = StringUtil.replaceUrl(url, authMap);

            return TrdPlatformBody.builder()
                    .url(url)
                    .header(null)
                    .method(method)
                    .body(null)
                    .bodyAnalysisType(apiBo.getBodyAnalysisType())
                    .bodyAnalysisCode(apiBo.getBodyAnalysisCode())
                    .build();
        }

        //对body的请求做再次加工
        Map<String, Object> finalBodyre = new LinkedHashMap<>(authMap);
        paramBos = paramBos.stream().sorted(Comparator.comparing(TrdPlatformApiParam::getId)) //排序问题
                .peek(param -> {
                    if (MapUtils.isNotEmpty(authMap) && authMap.containsKey(param.getParamKey())) {
                        Object auth = authMap.get(param.getParamKey());
                        param.setParamValue(auth);
                    }
                    if (param.getParamType() == TrdPlatformEnum.ParamTypeEnum.GROOVY.getCode()) {
                        Object obj = groovyShellCal(String.valueOf(param.getParamValue()), finalBodyre);
                        param.setParamValue(obj);
                        finalBodyre.put(param.getParamKey(), obj);
                    } else {
                        finalBodyre.put(param.getParamKey(), param.getParamValue());
                    }
                }).collect(Collectors.toList());

        Map<String, String> headerMap = paramBos.stream()
                .filter(res -> res.getParamPosition() == TrdPlatformEnum.ParamPositionEnum.HEAD.getCode())
                .collect(Collectors.toMap(TrdPlatformApiParam::getParamKey, param -> String.valueOf(param.getParamValue()), (oldValue, newValue) -> oldValue));

        Map<String, Object> bodyre = paramBos.stream()
                .filter(res -> res.getParamPosition() == TrdPlatformEnum.ParamPositionEnum.BODY.getCode() || res.getParamPosition() == TrdPlatformEnum.ParamPositionEnum.FORM.getCode())
                .collect(Collectors.toMap(TrdPlatformApiParam::getParamKey, TrdPlatformApiParam::getParamValue, (oldValue, newValue) -> oldValue));

        Map<String, Object> path = paramBos.stream()
                .filter(res -> res.getParamPosition() == TrdPlatformEnum.ParamPositionEnum.PATH.getCode() || res.getParamPosition() == TrdPlatformEnum.ParamPositionEnum.QUERY.getCode())
                .collect(Collectors.toMap(TrdPlatformApiParam::getParamKey, TrdPlatformApiParam::getParamValue, (oldValue, newValue) -> oldValue));

        url = StringUtil.replaceUrl(url, path);

        if (url.contains("%24%7B") || url.contains("${")) {
            url = StringUtil.replaceUrl(url, finalBodyre);
        }

        return TrdPlatformBody.builder()
                .url(url)
                .header(headerMap)
                .method(method)
                .body(bodyre)
                .bodyAnalysisType(apiBo.getBodyAnalysisType())
                .bodyAnalysisCode(apiBo.getBodyAnalysisCode())
                .build();
    }

    public static Object groovyShellCal(String groovyCode, Map<String, Object> bodyre) {
        // 创建一个绑定，用于存储变量
        Binding binding = new Binding();
        // 创建一个GroovyShell，用于执行Groovy代码
        GroovyShell shell = new GroovyShell(binding);

        // 设置变量
        if (bodyre != null) {
            bodyre.forEach(binding::setVariable);
        }

        // 执行代码
        Object res = shell.evaluate(groovyCode);
        return res;
    }

    @Override
    public void operateTaskWork(TrdPlatformTaskMessage task, Integer operate) {
        TimeJobManagerService timeJobManagerService = null;
        if ("local".equalsIgnoreCase(jobType)) {
            timeJobManagerService = ApplicationContextHelp.getBean(LocalJobManager.class);
        } else {
            timeJobManagerService = ApplicationContextHelp.getBean(IotXxlJobManager.class);
        }

        if (timeJobManagerService == null) {
            return;
        }

        if (StringUtils.isBlank(task.getFrequency()) || !isValidCronExpression(task.getFrequency())) {
            return;
        }

        String jobKey = TASK_JOB + task.getPlatformCode() + "-" + task.getTaskCode();
        String taskId = (String) redisTemplate.opsForValue().get(TASK_JOB + task.getPlatformCode() + "-" + task.getTaskCode());

        if (TrdPlatformEnum.ADD.getCode() == operate) {
            if (StringUtils.isNotBlank(taskId)) {
                log.info("{} {} {} {} 任务已经存在", task.getPlatformCode(), task.getTaskCode(), task.getFrequency(), taskId);
            } else {
                addTask(timeJobManagerService, task, jobKey);
            }
        }
        if (TrdPlatformEnum.UPDATE.getCode() == operate) {
            if (StringUtils.isNotBlank(taskId)) {
                removeTask(timeJobManagerService, task, jobKey, taskId);
            }

            addTask(timeJobManagerService, task, jobKey);
        }
        if (TrdPlatformEnum.REMOVE.getCode() == operate) {
            removeTask(timeJobManagerService, task, jobKey, taskId);
        }
    }

    private void updateTaskStatus(TrdPlatformTaskMessage task) {
        trdPlatformTaskRepository.updateTaskStatus(task.getPlatformCode(), task.getProductId(), task.getTaskCode(), TrdPlatformEnum.TaskStatusEnum.START.getCode());
    }

    private synchronized void addTask(TimeJobManagerService timeJobManagerService, TrdPlatformTaskMessage task, String jobKey) {
        String taskIdRe = timeJobManagerService.register(task.getTaskName(), task.getFrequency(), "CloudJob", task.getPlatformCode() + "," + task.getTaskCode());
        redisTemplate.opsForValue().set(jobKey, taskIdRe);
        log.info("{} {} {} 启动成功", task.getPlatformCode(), task.getTaskCode(), task.getFrequency());
        updateTaskStatus(task);
    }

    private synchronized void removeTask(TimeJobManagerService timeJobManagerService, TrdPlatformTaskMessage task, String jobKey, String taskId) {
        timeJobManagerService.unRegister(taskId);
        redisTemplate.delete(jobKey);
        log.info("{} {} 删除成功", task.getPlatformCode(), task.getTaskCode());
    }

    public static boolean isValidCronExpression(String cronExpression) {
        try {
            PatternParser.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, String> modelRef(String pCode, String productId, Object modelRefObj) {
        Map<String, String> modelRef = null;
        if (modelRefObj != null) {
            JSONObject metricData = JSONObject.parseObject(JSONObject.toJSONString(modelRefObj));
            modelRef = metricData.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> String.valueOf(entry.getValue()),
                            (oldValue, newValue) -> oldValue
                    ));
        }
        if (StringUtils.isNotBlank(productId)) {
            try {
                TrdPlatformModelRefBo modelRefBo = trdPlatformModelRefRepository.queryByCode(pCode, productId);
                if (modelRefBo != null) {
                    List<TrdPlatformMeasureRefBo> refBos = trdPlatformMeasureRefRepository.queryById(modelRefBo.getId());
                    if (CollectionUtils.isNotEmpty(refBos)) {
                        return refBos.stream().collect(Collectors.toMap(TrdPlatformMeasureRefBo::getPlatformMeasureCode, TrdPlatformMeasureRefBo::getEnnMeasureCode, (o, n) -> o));
                    } else {
                        return modelRef;
                    }
                }
            } catch (Exception e) {
                log.warn("EnnewDeviceContext modelRef exception {}", e.getMessage());
            }
        } else {
            return modelRef;
        }
        return null;
    }

    @Override
    public String modelRefMetric(String orgMetric, Map<String, String> modelRef) {
        if (MapUtils.isNotEmpty(modelRef)) {
            if (modelRef.containsKey(orgMetric)) {
                return modelRef.get(orgMetric);
            }
        }
        return orgMetric;
    }

}
