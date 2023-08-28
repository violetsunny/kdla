/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.exception;

import top.kdla.framework.dto.ErrorCodeI;

/**
 * @author kanglele
 * @version $Id: LockFailException, v 0.1 2023/8/17 16:37 kanglele Exp $
 */
public class LockFailException extends BaseException {
    private static final long serialVersionUID = -3816322588329303318L;

    private static final String DEFAULT_ERR_CODE = "LOCK_ERROR";

    public LockFailException(String message) {
        super(DEFAULT_ERR_CODE, message);
    }

    public LockFailException(String code, String message) {
        super(code, message);
    }

    public LockFailException(ErrorCodeI errCode) {
        this(errCode.getCode(), errCode.getMsg());
    }

}
