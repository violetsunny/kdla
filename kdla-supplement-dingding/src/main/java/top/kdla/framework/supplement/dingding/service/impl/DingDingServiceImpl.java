/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.service.impl;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.kdla.framework.common.enums.ContentTypeEnum;
import top.kdla.framework.common.enums.MsgTypeEnum;
import top.kdla.framework.common.utils.DateUtil;
import top.kdla.framework.supplement.dingding.config.DingAlertConfig;
import top.kdla.framework.supplement.dingding.enums.MsgSendRespEnum;
import top.kdla.framework.supplement.dingding.model.req.DingDingMessage;
import top.kdla.framework.supplement.dingding.model.req.DingMessageAt;
import top.kdla.framework.supplement.dingding.model.req.MarkdownMessage;
import top.kdla.framework.supplement.dingding.model.req.TextMessage;
import top.kdla.framework.supplement.dingding.model.res.DingDingAlertResult;
import top.kdla.framework.supplement.dingding.service.DingDingService;
import top.kdla.framework.supplement.dingding.utils.DingdingHttpUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author kanglele
 * @version $Id: DingDingServiceImpl, v 0.1 2023/2/2 14:26 kanglele Exp $
 */
@Service
public class DingDingServiceImpl implements DingDingService {
    private static final Logger logger = LoggerFactory.getLogger(DingDingServiceImpl.class);
    @Resource
    private DingAlertConfig dingAlertConfig;

    public DingDingServiceImpl() {
    }

    public String sendTextMessage(String msg) {
        try {
            if (!this.dingAlertConfig.isDingAlertEnable()) {
                logger.info("DingDingServiceImpl.sendMessage#no need send.");
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder sb = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.TEXT.getCode(), new TextMessage(sb.toString()), new DingMessageAt((List)null, true));
            String rest = DingdingHttpUtil.doPost(this.dingAlertConfig.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
            DingDingAlertResult result = JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                logger.info("DingDingServiceImpl.sendMessage failed#msg:{}, rest:{}", msg, rest);
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var6) {
            logger.warn("钉钉预警异常", var6);
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }

    public String sendTextMessage(String msg, List<String> atMobiles) {
        try {
            if (!this.dingAlertConfig.isDingAlertEnable()) {
                logger.info("DingDingServiceImpl.sendMessage#no need send.");
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder sb = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.TEXT.getCode(), new TextMessage(sb.toString()), new DingMessageAt(atMobiles, false));
            String rest = DingdingHttpUtil.doPost(this.dingAlertConfig.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
            DingDingAlertResult result = (DingDingAlertResult)JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                logger.info("DingDingServiceImpl.sendMessage failed#msg:{},atMobiles:{}, rest:{}", new Object[]{msg, atMobiles, rest});
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var7) {
            logger.warn("钉钉预警异常", var7);
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }

    private StringBuilder initDingdingMessage(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.dingAlertConfig.getDingAlertProperties().getDingKeywords());
        sb.append("【" + this.dingAlertConfig.getDingAlertProperties().getAppName() + "】");
        sb.append("【" + DateUtil.format(new Date()) + "】");
        sb.append("【" + this.dingAlertConfig.getEnv() + "】");
        sb.append(msg);
        return sb;
    }

    public String sendMessage(String title, String msg) {
        try {
            if (!this.dingAlertConfig.isDingAlertEnable()) {
                logger.info("DingDingServiceImpl.sendMessage#no need send.");
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder content = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.MARKDOWN.getCode(), new MarkdownMessage(title, content.toString()), new DingMessageAt((List)null, true));
            String rest = DingdingHttpUtil.doPost(this.dingAlertConfig.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
            DingDingAlertResult result = (DingDingAlertResult)JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                logger.info("DingDingServiceImpl.sendMessage failed#title:{}, msg:{}, rest:{}", new Object[]{title, msg, rest});
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var7) {
            logger.warn("钉钉预警异常", var7);
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }

    public String sendMessage(String title, String msg, List<String> atMobiles) {
        try {
            if (!this.dingAlertConfig.isDingAlertEnable()) {
                logger.info("DingDingServiceImpl.sendMessage#no need send.");
                return MsgSendRespEnum.NO_NEED.getCode();
            }

            StringBuilder content = this.initDingdingMessage(msg);
            DingDingMessage dingDingMessage = new DingDingMessage(MsgTypeEnum.MARKDOWN.getCode(), new MarkdownMessage(title, content.toString()), new DingMessageAt(atMobiles, false));
            String rest = DingdingHttpUtil.doPost(this.dingAlertConfig.getDingUrl(), JSON.toJSONString(dingDingMessage), ContentTypeEnum.APPLICATION_JSON, 3000);
            DingDingAlertResult result = (DingDingAlertResult)JSON.parseObject(rest, DingDingAlertResult.class);
            if (Objects.isNull(result) || !result.isSuccess()) {
                logger.info("DingDingServiceImpl.sendMessage failed#title:{}, msg:{},atMobiles:{}, rest:{}", new Object[]{title, msg, atMobiles, rest});
                return MsgSendRespEnum.FAIL.getCode();
            }
        } catch (Exception var8) {
            logger.warn("钉钉预警异常", var8);
            return MsgSendRespEnum.FAIL.getCode();
        }

        return MsgSendRespEnum.OK.getCode();
    }
}
