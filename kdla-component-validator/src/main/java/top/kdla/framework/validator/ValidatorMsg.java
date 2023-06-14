/**
 * kanglele Inc. Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.validator;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 校验类
 *
 * @author kanglele
 * @version $Id: ValidatorMsg, v 0.1 2021/10/9 15:14 Exp $
 */
public class ValidatorMsg {

    private Boolean result;

    private String resultMsg;

    private List<String> resultMsgList;

    public ValidatorMsg() {
        this.result = true;
    }

    public ValidatorMsg(List<String> resultMsgList) {
        this.result = true;
        this.resultMsgList = resultMsgList;
    }

    /**
     * 按照逻辑校验后，输出信息 booleanSupplier有一次错误就不会再执行，resultMsg只返回第一次报错的信息。
     *
     * @param booleanSupplier
     * @param msg
     * @return
     */
    public ValidatorMsg resultMsg(BooleanSupplier booleanSupplier, String msg) {
        if (this.result) {
            this.result = !booleanSupplier.getAsBoolean();
            this.resultMsg = this.result ? null : msg;
        }
        return this;
    }

    /**
     * 按照逻辑校验后，输出信息 booleanSupplier每一个都执行，resultMsgList将所有错误信息收集。
     *
     * @param booleanSupplier
     * @param msg
     * @return
     */
    public ValidatorMsg resultMsgList(BooleanSupplier booleanSupplier, String msg) {
        if (null == resultMsgList) {
            throw new NullPointerException("resultMsgList need init list");
        }
        Boolean res = !booleanSupplier.getAsBoolean();
        if (this.result) {
            this.result = res;
        }
        if (!res) {
            resultMsgList.add(msg);
        }
        return this;
    }

    public String getMsg() {
        return this.resultMsg;
    }

    public List<String> getMsgList() {
        return this.resultMsgList;
    }

    public Boolean result() {
        return this.result;
    }
}
