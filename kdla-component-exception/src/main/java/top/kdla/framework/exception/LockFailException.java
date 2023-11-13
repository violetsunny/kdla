/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.exception;

import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.dto.exception.ErrorCodeI;

/**
 * @author kanglele
 * @version $Id: LockFailException, v 0.1 2023/8/17 16:37 kanglele Exp $
 */
public class LockFailException extends BaseException {
    private static final long serialVersionUID = -3816322588329303318L;

    public LockFailException(String message) {
        super(ErrorCode.LOCK_ERROR.getCode(), message);
    }

    public LockFailException(String code, String message) {
        super(code, message);
    }

    public LockFailException(ErrorCodeI errCode) {
        this(errCode.getCode(), errCode.getMsg());
    }

}
