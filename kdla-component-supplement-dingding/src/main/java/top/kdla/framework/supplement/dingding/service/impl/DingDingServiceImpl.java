/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.kdla.framework.common.enums.ContentTypeEnum;
import top.kdla.framework.common.enums.MsgTypeEnum;
import top.kdla.framework.supplement.dingding.config.DingAlertConfigure;
import top.kdla.framework.supplement.dingding.enums.MsgSendRespEnum;
import top.kdla.framework.supplement.dingding.model.req.DingDingMessage;
import top.kdla.framework.supplement.dingding.model.req.DingMessageAt;
import top.kdla.framework.supplement.dingding.model.req.MarkdownMessage;
import top.kdla.framework.supplement.dingding.model.req.TextMessage;
import top.kdla.framework.supplement.dingding.model.res.DingDingAlertResult;
import top.kdla.framework.supplement.dingding.service.DingDingService;
import top.kdla.framework.supplement.dingding.utils.DingDingHttpUtil;
import top.kdla.framework.supplement.http.VertxHttpClient;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author kanglele
 * @version $Id: DingDingServiceImpl, v 0.1 2023/2/2 14:26 kanglele Exp $
 */
@Service
@Slf4j
public class DingDingServiceImpl implements DingDingService {

    @Resource
    private DingAlertConfigure dingAlertConfigure;
    @Resource
    private VertxHttpClient vertxHttpClient;

    @Override
    public String sendTextMessage(String msg) {
        try {
            if (!this.dingAlertConfigure.isDingAlertEnable()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage#no need send.");
                }
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder sb = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.TEXT.getCode(), new TextMessage(sb.toString()), new DingMessageAt(null, true));
            CompletableFuture<DingDingAlertResult> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, this.dingAlertConfigure.getDingUrl(), null, dingDingMessage, DingDingAlertResult.class);
            DingDingAlertResult result = futureRes.get();
//            String rest = DingDingHttpUtil.doPost(this.dingAlertConfigure.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
//            DingDingAlertResult result = JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage failed#msg:{}, rest:{}", msg, JSON.toJSONString(result));
                }
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var6) {
            if (log.isWarnEnabled()) {
                log.warn("钉钉预警异常", var6);
            }
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }

    @Override
    public String sendTextMessage(String msg, List<String> atMobiles) {
        try {
            if (!this.dingAlertConfigure.isDingAlertEnable()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage#no need send.");
                }
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder sb = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.TEXT.getCode(), new TextMessage(sb.toString()), new DingMessageAt(atMobiles, false));
            CompletableFuture<DingDingAlertResult> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, this.dingAlertConfigure.getDingUrl(), null, dingDingMessage, DingDingAlertResult.class);
            DingDingAlertResult result = futureRes.get();
//            String rest = DingDingHttpUtil.doPost(this.dingAlertConfigure.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
//            DingDingAlertResult result = JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage failed#msg:{},atMobiles:{}, rest:{}", msg, atMobiles, JSON.toJSONString(result));
                }
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var7) {
            if (log.isWarnEnabled()) {
                log.warn("钉钉预警异常", var7);
            }
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }

    private StringBuilder initDingdingMessage(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.dingAlertConfigure.getDingAlertProperties().getDingKeywords());
        sb.append("【").append(this.dingAlertConfigure.getDingAlertProperties().getAppName()).append("】");
        sb.append("【").append(DateUtil.formatDateTime(new Date())).append("】");
        sb.append("【").append(this.dingAlertConfigure.getEnv()).append("】");
        sb.append(msg);
        return sb;
    }

    @Override
    public String sendMessage(String title, String msg) {
        try {
            if (!this.dingAlertConfigure.isDingAlertEnable()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage#no need send.");
                }
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder content = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.MARKDOWN.getCode(), new MarkdownMessage(title, content.toString()), new DingMessageAt((List) null, true));
            CompletableFuture<DingDingAlertResult> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, this.dingAlertConfigure.getDingUrl(), null, dingDingMessage, DingDingAlertResult.class);
            DingDingAlertResult result = futureRes.get();
            //            String rest = DingDingHttpUtil.doPost(this.dingAlertConfigure.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
//            DingDingAlertResult result = JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage failed#title:{}, msg:{}, rest:{}", title, msg, JSON.toJSONString(result));
                }
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var7) {
            if (log.isWarnEnabled()) {
                log.warn("钉钉预警异常", var7);
            }
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }

    @Override
    public String sendMessage(String title, String msg, List<String> atMobiles) {
        try {
            if (!this.dingAlertConfigure.isDingAlertEnable()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage#no need send.");
                }
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder content = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.MARKDOWN.getCode(), new MarkdownMessage(title, content.toString()), new DingMessageAt(atMobiles, false));
            CompletableFuture<DingDingAlertResult> futureRes = vertxHttpClient.sendRequest(HttpMethod.POST, this.dingAlertConfigure.getDingUrl(), null, dingDingMessage, DingDingAlertResult.class);
            DingDingAlertResult result = futureRes.get();
//            String rest = DingDingHttpUtil.doPost(this.dingAlertConfigure.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
//            DingDingAlertResult result = JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                if (log.isInfoEnabled()) {
                    log.info("DingDingServiceImpl.sendMessage failed#title:{}, msg:{},atMobiles:{}, rest:{}", title, msg, atMobiles, JSON.toJSONString(result));
                }
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var8) {
            if (log.isWarnEnabled()) {
                log.warn("钉钉预警异常", var8);
            }
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }
}
