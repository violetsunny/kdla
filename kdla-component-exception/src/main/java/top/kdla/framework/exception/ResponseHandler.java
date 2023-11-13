package top.kdla.framework.exception;

import top.kdla.framework.dto.Response;

/**
 * ResponseHandler
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public class ResponseHandler {

    public static Object handle(Class returnType, String code, String message) {
        //TODO other response
        if (isKdlaResponse(returnType)) {
            return handleKdlaResponse(returnType, code, message);
        }
        return Response.buildFailure(code, message);
    }

    public static Object handle(Class returnType, BaseException e) {
        return handle(returnType, e.getCode(), e.getMessage());
    }

    private static Object handleKdlaResponse(Class returnType, String code, String message) {
        try {
            Response response = (Response)returnType.newInstance();
            response.setSuccess(false);
            response.setCode(code);
            response.setMsg(message);
            return response;
        } catch (Exception ex) {
            return Response.buildFailure(code, message);
        }
    }

    private static boolean isKdlaResponse(Class returnType) {
        return returnType == Response.class || returnType.getGenericSuperclass() == Response.class;
    }
}
