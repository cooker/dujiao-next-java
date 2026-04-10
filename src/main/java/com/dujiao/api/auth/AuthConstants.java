package com.dujiao.api.auth;

public final class AuthConstants {

    public static final String PURPOSE_REGISTER = "register";
    public static final String PURPOSE_RESET_PASSWORD = "reset_password";
    public static final String PURPOSE_CHANGE_EMAIL_OLD = "change_email_old";
    public static final String PURPOSE_CHANGE_EMAIL_NEW = "change_email_new";
    /** 与 Go {@code VerifyPurposeTelegramBind} 一致：渠道绑定已有账号。 */
    public static final String PURPOSE_TELEGRAM_BIND = "telegram_bind";

    public static final String OAUTH_PROVIDER_TELEGRAM = "telegram";

    public static final String EMAIL_CHANGE_MODE_BIND_ONLY = "bind_only";
    public static final String EMAIL_CHANGE_MODE_CHANGE_WITH_OLD_AND_NEW = "change_with_old_and_new";

    public static final String PASSWORD_CHANGE_MODE_SET_WITHOUT_OLD = "set_without_old";
    public static final String PASSWORD_CHANGE_MODE_CHANGE_WITH_OLD = "change_with_old";

    private AuthConstants() {}
}
