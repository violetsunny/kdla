package top.kdla.framework.catchlog;

import lombok.extern.slf4j.Slf4j;
import top.kdla.framework.dto.Response;
import top.kdla.framework.exception.BaseException;

/**
 * ResponseHandler
 *
 * @author vincent.li
 * @since 2021/7/9 14:15
 */
@Slf4j
public class ResponseHandler {

    public static Object handle(Class returnType, String code, String message) {
        //TODO other response

        if (iskdlaResponse(returnType)) {
            return handlekdlaResponse(returnType, code, message);
        }

        return Response.buildFailure(code, message);
    }

    public static Object handle(Class returnType, BaseException e) {
        return handle(returnType, e.getCode(), e.getMessage());
    }

    private static Object handlekdlaResponse(Class returnType, String code, String message) {
        try {
            Response response = (Response)returnType.newInstance();
            response.setSuccess(false);
            response.setCode(code);
            response.setMessage(message);
            return response;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return Response.buildFailure(code, message);
        }
    }

    private static boolean iskdlaResponse(Class returnType) {
        return returnType == Response.class || returnType.getGenericSuperclass() == Response.class;
    }
}
