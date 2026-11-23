package top.kdla.framework.supplement.cache.cache;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * @author dongguo.tao
 * @description
 * @date 2021-09-23 15:21:15
 */
@Slf4j
public class MachineIdHelp {

    private final SpringRedisHelp redisHelp;

    private final String appId;

    public MachineIdHelp(SpringRedisHelp redisHelp, String appId) {
        this.redisHelp = redisHelp;
        this.appId = appId;
        this.init();
    }

    private static final Integer MAX_ID = 31;

    private static final Integer MIN_ID = 0;

    /**
     * 机器ID注册记录的过期时间（毫秒），与旧实现3600秒语义一致
     */
    private static final long REGISTER_EXPIRE_MILLIS = 3600L * 1000;

    /**
     * 机器id
     */
    private static volatile Integer machineId;

    private static volatile Integer dataCenterId = 0;

    /**
     * 本地ip地址
     */
    private static String localIp;

    /**
     * hash机器IP初始化一个机器ID
     */
    public SnowflakeGenerator getGenerator() {
        if (Objects.isNull(machineId) || Objects.isNull(dataCenterId)) {
            //创建一个机器ID
            getRandomMachineAndDataCenterId();
        }
        if (log.isInfoEnabled()) {
            log.info("初始化 machine_id:{},data_center_id:{},localIp:{}", machineId, dataCenterId, localIp);
        }
        return new SnowflakeGenerator(machineId, dataCenterId);
    }

    /**
     * 获取ip地址
     *
     * @return
     * @throws UnknownHostException
     */
    private String getIPAddress() throws UnknownHostException {
        InetAddress address = InetAddress.getLocalHost();
        String ip = address.getHostAddress();
        localIp = ip;
        String ipStr = ip.replaceAll("\\.", "&");
        if (log.isInfoEnabled()) {
            log.info("MachineIdConfig.getIPAddress ip={},ipStr={}", ip, ipStr);
        }
        return ipStr;
    }

    private void init() {
        if (log.isInfoEnabled()) {
            log.info("MachineIdConfig init start....");
        }
        long start = System.currentTimeMillis();
        try {
            String ip = getIPAddress();
            for (int dataCenter = MIN_ID; dataCenter <= MAX_ID; dataCenter++) {
                for (int machine = MIN_ID; machine <= MAX_ID; machine++) {
                    MutablePair<String, String> keyPair = getKeyPair(machine, dataCenter);
                    String key = keyPair.left + keyPair.right;
                    // SETNX原子注册，消除旧实现get判空后再set的check-then-act竞态（并发下多实例会抢到同一ID）；
                    // 注册失败说明该ID已被占用，换下一个ID重试
                    if (redisHelp.lock(key, ip, REGISTER_EXPIRE_MILLIS)) {
                        // 注册成功才发布到静态字段：失败路径（Redis异常/槽耗尽）不残留脏值，
                        // 保证getGenerator判空命中走随机回退+ERROR告警，而非静默用未注册ID发号撞号
                        machineId = machine;
                        dataCenterId = dataCenter;
                        if (log.isInfoEnabled()) {
                            log.info("MachineIdConfig.init ip:{},machineId:{},dataCenterId:{}, usedTime : {}", ip, machineId, dataCenterId, (System.currentTimeMillis() - start));
                        }
                        return;
                    }
                }
            }
            log.error("MachineIdConfig注册失败:1024槽耗尽,appId:{}", appId);
        } catch (Exception e) {
            log.error("MachineIdConfig init failed. machineId:{},dataCenterId:{}", machineId, dataCenterId, e);
        }
    }

    /**
     * 容器销毁前清除注册记录
     */
    public void destroyMachineId() {
        if (machineId == null || dataCenterId == null) {
            return;
        }
        MutablePair<String, String> keyPair = getKeyPair(machineId, dataCenterId);
        try {
            String key = keyPair.left + keyPair.right;
            //旧实现set(key,空串,1秒)：1秒窗口内key仍存在(空值)会阻塞其他实例SETNX注册；改为del立即删除，消除窗口
            redisHelp.del(key);
            if (log.isInfoEnabled()) {
                log.info("MachineIdConfig destroy start.... key:{}", keyPair.left);
            }
        } catch (Exception e) {
            log.error("destroyMachineId failed. key={}", keyPair.left, e);
        }
    }


    private MutablePair<String, String> getKeyPair(int machineId, int dataCenterId) {
        return MutablePair.of(appId + "::snowflake::", machineId + "&" + dataCenterId);
    }


    /**
     * 获取1~31随机数
     */
    private void getRandomMachineAndDataCenterId() {
        machineId = (int) (Math.random() * 32);
        dataCenterId = (int) (Math.random() * 32);
        // 回退随机取号未经redis SETNX注册，多实例可能撞号导致雪花ID重复，显式告警便于监控发现
        log.error("MachineIdConfig回退随机取号(未注册) machineId={},dataCenterId={}，存在跨实例雪花ID冲突风险", machineId, dataCenterId);
    }

}
