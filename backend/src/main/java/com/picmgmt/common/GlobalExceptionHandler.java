package com.picmgmt.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException e) {
        return response(HttpStatus.UNAUTHORIZED, Result.error(401, "请先登录"));
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Void>> handleNotPermission(NotPermissionException e) {
        return response(HttpStatus.FORBIDDEN, Result.error(403, "无权执行此操作"));
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Result<Void>> handleNotRole(NotRoleException e) {
        return response(HttpStatus.FORBIDDEN, Result.error(403, "无权访问后台管理系统"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return response(HttpStatus.BAD_REQUEST, Result.error(400, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        return response(HttpStatus.BAD_REQUEST, Result.error(400, msg));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return response(httpStatus(e.getErrorCode()), Result.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, Result.error(413, "文件大小超过20MB限制"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, Result.error(500, "服务器内部错误"));
    }

    private HttpStatus httpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, IMAGE_PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, IMAGE_NOT_FOUND, USER_NOT_FOUND, CATEGORY_NOT_FOUND,
                    COMMENT_NOT_FOUND, NOTIFICATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, USERNAME_EXISTS, EMAIL_EXISTS, PHONE_EXISTS,
                    CATEGORY_NAME_EXISTS -> HttpStatus.CONFLICT;
            case IMAGE_SIZE_EXCEEDED -> HttpStatus.PAYLOAD_TOO_LARGE;
            case INTERNAL_ERROR, STORAGE_UPLOAD_FAILED, STORAGE_DOWNLOAD_FAILED ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private ResponseEntity<Result<Void>> response(HttpStatus status, Result<Void> body) {
        return ResponseEntity.status(status).body(body);
    }
}
