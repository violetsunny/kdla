package com.xxl.job.admin.core.alarm.impl;

import com.google.gson.JsonObject;
import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * job alarm by email
 *
 * @author xuxueli 2020-01-19
 */
@Component
public class IcomeJobAlarm implements JobAlarm {
    private static Logger logger = LoggerFactory.getLogger(IcomeJobAlarm.class);


    @Value("${alarm.icomeRobotUrl:https://oapi.dingtalk.com/robot/send?access_token=264848e6c70b4fcf05b2095e724e4348e90ecbb634e5f159dff68db8cb7c1344}")
    private String icomeRobotUrl = "https://oapi.dingtalk.com/robot/send?access_token=264848e6c70b4fcf05b2095e724e4348e90ecbb634e5f159dff68db8cb7c1344";

    @Value("${xxl.alarm.count:10}")
    private Integer alarmCount;
    @Value("${xxl.alarm.rate:50}")
    private Integer alarmRate;

    @Value("${xxl.alarm.env:dev}")
    private String alarmEnv;

    /**
     * fail alarm
     *
     * @param jobLog
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        boolean alarmResult = true;

        // send monitor email
        if (info != null && info.getAuthor() != null && info.getAuthor().trim().length() > 0) {


            // email info
            XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(Integer.valueOf(info.getJobGroup()));


            String title = "job告警";
            String alarmContent = buildAlarmContent(info, jobLog, group);
            // make mail
            try {
                // 内容为空时无需发送
                if (StringUtils.isNotBlank(alarmContent)) {
                    sendIcomeRobot(title, alarmContent);
                }
            } catch (Exception e) {
                logger.error(">>>>>>>>>>> xxl-job, job fail alarm icome send error, JobLogId:{}", jobLog.getId(), e);

                alarmResult = false;
            }

        }

        return alarmResult;
    }


    /**
     * 发送icome告警信息
     */
    private void sendIcomeRobot(String title, String contentInfo) {

        if (StringUtils.isBlank(contentInfo)) {
            return;
        }
        JsonObject content = new JsonObject();
        content.addProperty("title", title);
        content.addProperty("text", contentInfo);
        JsonObject req = new JsonObject();
        req.addProperty("msgtype", "markdown");
        req.add("markdown", content);

        try {
            ReturnT returnT = XxlJobRemotingUtil.postBody(icomeRobotUrl, "", 3, req, String.class);
            logger.info("指令异常监控发送结果：{}", returnT);
        } catch (Exception e) {
            logger.error("指令异常监控发送异常:", e.fillInStackTrace());
        }
    }


    /**
     * 创建告警内容
     *
     * @param info
     * @param jobLog
     * @param group
     * @return
     */
    private String buildAlarmContent(XxlJobInfo info, XxlJobLog jobLog, XxlJobGroup group) {
        Long failCount = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findFailCount(jobLog.getJobId());
        Long totalCount = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findTotalCount(jobLog.getJobId());
        Date startDate = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findStartDate(jobLog.getJobId());
        double failRate = (double) failCount / totalCount * 100;

        if (totalCount < alarmCount*2 && failCount < alarmCount && failRate < alarmRate && failCount > 3) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 【"+alarmEnv+"】job告警" + group.getAppname() + "的" + info.getJobDesc() + "运行失败，请及时处理！\n");
        sb.append("* 调度任务Id：" + jobLog.getJobId() + "\n");
        sb.append("* 调度执行器：" + info.getExecutorHandler() + "\n");
        sb.append("* 调度日志Id：" + jobLog.getId() + "\n");
        sb.append("* 调度时间：" + DateUtil.format(jobLog.getTriggerTime(), "yyyy-MM-dd HH:mm:ss") + "\n");
        sb.append("* 处理时间：" + jobLog.getHandleTime() + "\n");
        sb.append("* 最近一小时首次触发时间：" + DateUtil.format(startDate, "yyyy-MM-dd HH:mm:ss") + "\n");
        sb.append("* 最近一小时总计运行：" + totalCount + "次\n");
        sb.append("* 最近一小时运行失败：" + failCount + "次\n");
        sb.append("* 最近一小时失败率：" + String.format("%.2f", failRate) + "%\n");
        sb.append("> 最新执行记录：(调度结果:" + jobLog.getTriggerCode() + ")(执行结果:" + jobLog.getHandleCode() + ")(执行结果消息：" + jobLog.getHandleMsg() + ")\n");
        sb.append("* 触发详情：\n" +
                " > " + jobLog.getTriggerMsg() + "\n");
        sb.append("* 负责人：" + info.getAuthor() + "\n");

        return sb.toString();
    }


}
