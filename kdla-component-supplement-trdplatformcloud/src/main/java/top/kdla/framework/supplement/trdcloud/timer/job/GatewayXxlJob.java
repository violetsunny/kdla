/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.trdcloud.timer.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.kdla.framework.supplement.job.context.XxlJobHelper;
import top.kdla.framework.supplement.trdcloud.timer.annotation.EnnIotXxlJob;
import top.kdla.framework.supplement.trdcloud.timer.handler.EnnIotXxlJobHandler;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 云云对接
 *
 * @author kanglele
 * @version $Id: HttpCloudJob, v 0.1 2023/5/17 16:55 kanglele Exp $
 */
@Slf4j
@Component
@EnnIotXxlJob("GatewayXxlJob")
public class GatewayXxlJob extends EnnIotXxlJobHandler {

    @Override
    public   boolean doExecute(String jobParam) throws Exception {

        log.info("http poll job start,time:{},params:{}", new Date(), jobParam);
        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at:" + i);
            TimeUnit.SECONDS.sleep(2);
        }
        return true;
    }




    /**
     * （Bean模式）
     */
//    @XxlJob("HttpPollJobHandler")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("HttpPollJobHandler,start.");
        String jobParam = XxlJobHelper.getJobParam();
        log.info("http poll job start,time:{},params:{}", new Date(), jobParam);
        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at:" + i);
            TimeUnit.SECONDS.sleep(2);
        }
        // default success
    }

    /**
     * 2、分片广播任务 实例
     */
//    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 业务逻辑
        for (int i = 0; i < shardTotal; i++) {
            if (i == shardIndex) {
                XxlJobHelper.log("第 {} 片, 命中分片开始处理", i);
            } else {
                XxlJobHelper.log("第 {} 片, 忽略", i);
            }
        }

    }
}
