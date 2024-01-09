package top.kdla.framework.supplement.monitor.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.exporter.PushGateway;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
//@EnableScheduling
public class PushGatewayManager {

    @Value("${spring.application.name}")
    private String appId;

    @Value("${spring.profiles.active}")
    private String env;

    private Map<String, String> groupingKey;

    /**
     * 测点注册中心
     * 统一注册组件内的所有测点 以统一上报
     */
    @Getter
    private final CollectorRegistry registry = new CollectorRegistry();

    @Value("${prometheus.pushgateway.url:10.39.64.13:9091}")
    private String pushGatewayUrl;

    private PushGateway pushGateway;

    @Value("${prometheus.service.port:0000}")
    private int port;
    @Value("${prometheus.service.switch:false}")
    private boolean service;
    @Getter
    private HTTPServer server;

    @PostConstruct
    private void pushGatewayInit() throws Exception {
        if (service) {
            //在此开启一个http端口，暴露给Prometheus调用
            server = new HTTPServer(port);
        } else {
            //推送url
            pushGateway = new PushGateway(pushGatewayUrl);
        }

        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            ip = "";
        }
        String node = ip;
        groupingKey = new HashMap<String, String>() {{
            put("instance", node);
            put("env", env);
        }};
    }

    //@Scheduled(fixedRate = 5000)
    private void pushAllByTime() {
        pushAdd(registry);
    }

    public void pushAdd(CollectorRegistry registry) {
        try {
            if (pushGateway != null) {
                pushGateway.pushAdd(registry, appId, groupingKey);
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("pushgateway 推送失败", e);
            }
        }

    }

    public void pushAdd(Collector collector) {
        try {
            if (pushGateway != null) {
                pushGateway.pushAdd(collector, appId, groupingKey);
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("pushgateway 推送失败", e);
            }
        }
    }
}
