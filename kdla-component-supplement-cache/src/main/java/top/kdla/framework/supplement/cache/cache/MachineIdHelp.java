package top.kdla.framework.supplement.cache.cache;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        log.info("初始化 machine_id:{},data_center_id:{},localIp:{}", machineId, dataCenterId, localIp);
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
        String ipStr = ip.replaceAll("\\.", "&");
        log.info("MachineIdConfig.getIPAddress ip={},ipStr={}", ip, ipStr);
        return ipStr;
    }

    private void init() {
        log.info("MachineIdConfig init start....");
        long start = System.currentTimeMillis();
        try {
            String ip = getIPAddress();
            for (int dataCenter = MIN_ID; dataCenter <= MAX_ID; dataCenter++) {
                for (int machine = MIN_ID; machine <= MAX_ID; machine++) {
                    machineId = machine;
                    dataCenterId = dataCenter;
                    MutablePair<String, String> keyPair = getKeyPair(machineId, dataCenterId);
                    String key = keyPair.left + keyPair.right;
                    String cacheValue = (String) redisHelp.get(key);
                    if (StringUtils.isBlank(cacheValue)) {
                        redisHelp.set(key, ip, 3600L);
                        log.info("MachineIdConfig.init ip:{},machineId:{},dataCenterId:{}, usedTime : {}", ip, machineId, dataCenterId, (System.currentTimeMillis() - start));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("MachineIdConfig init failed. machineId:{},dataCenterId:{}", machineId, dataCenterId, e);
        }
    }

    /**
     * 容器销毁前清除注册记录
     */
    public void destroyMachineId() {
        MutablePair<String, String> keyPair = getKeyPair(machineId, dataCenterId);
        try {
            String key = keyPair.left + keyPair.right;
            redisHelp.set(key, StringUtils.EMPTY, 1L);
        } catch (Exception e) {
            log.error("destroyMachineId failed. key={}", keyPair.left, e);
        }
        log.info("MachineIdConfig destroy start.... key:{}", keyPair.left);
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
        log.info("MachineIdConfig.getRandomMachineAndDataCenterId machineId={},dataCenterId={}", machineId, dataCenterId);
    }

}
