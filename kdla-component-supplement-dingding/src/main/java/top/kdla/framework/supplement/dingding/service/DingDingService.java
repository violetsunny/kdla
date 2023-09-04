/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.service;

import java.util.List;

/**
 * @author kanglele
 * @version $Id: DingDingService, v 0.1 2023/2/2 14:25 kanglele Exp $
 */
public interface DingDingService {

    String sendTextMessage(String var1);

    String sendTextMessage(String var1, List<String> var2);

    String sendMessage(String var1, String var2);

    String sendMessage(String var1, String var2, List<String> var3);
}
