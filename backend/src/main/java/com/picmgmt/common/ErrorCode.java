package com.picmgmt.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // Image errors (1xxx)
    IMAGE_NOT_FOUND(1001, "图片不存在"),
    IMAGE_FORMAT_INVALID(1002, "仅支持 JPG/PNG/WEBP 格式"),
    IMAGE_SIZE_EXCEEDED(1003, "图片大小不能超过20MB"),
    IMAGE_DESCRIPTION_TOO_LONG(1004, "描述不能超过500个字符"),
    IMAGE_PERMISSION_DENIED(1005, "无权操作该图片"),
    IMAGE_VISIBILITY_INVALID(1006, "可见权限参数无效"),

    // User errors (2xxx)
    USER_NOT_FOUND(2001, "用户不存在"),
    USERNAME_EXISTS(2002, "用户名已存在"),
    PASSWORD_MISMATCH(2003, "两次密码不一致"),
    LOGIN_FAILED(2004, "用户名或密码错误"),
    EMAIL_EXISTS(2005, "该邮箱已被其他用户使用"),
    PHONE_EXISTS(2006, "该手机号已被其他用户使用"),
    EMAIL_NOT_BOUND(2007, "该邮箱未绑定任何账号"),
    CODE_SEND_FAILED(2008, "验证码发送失败"),
    CODE_INVALID(2009, "验证码错误或已过期"),
    CODE_TOO_FREQUENT(2010, "验证码发送过于频繁，请60秒后重试"),
    CAPTCHA_INVALID(2012, "图形验证码错误或已过期"),
    USER_DELETED(2014, "用户不存在或已注销"),

    // Category errors (3xxx)
    CATEGORY_NOT_FOUND(3001, "分类不存在"),
    CATEGORY_NAME_EXISTS(3002, "分类名称已存在"),
    CATEGORY_NAME_TOO_LONG(3003, "分类名称不能超过20个字符"),

    // Comment errors (4xxx)
    COMMENT_NOT_FOUND(4001, "评论不存在"),
    COMMENT_EMPTY(4002, "评论内容不能为空"),

    // Storage errors (5xxx)
    STORAGE_UPLOAD_FAILED(5001, "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED(5002, "文件下载失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
