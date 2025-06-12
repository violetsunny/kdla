package top.kdla.framework.supplement.timer.manager;

import java.util.Map;

public interface TimeJobManagerService {

    String addJob(String name,String cron, String handler, String param);

    Boolean removeJob(String id);

    Boolean startJob(String id);

    Boolean stopJob(String id);

    Map<String, Object> pageList(int start, int length, int triggerStatus);

    String register(String name,String cron, String handler, String param);

    Boolean unRegister(String id);
}
