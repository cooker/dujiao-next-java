package com.dujiao.api.service.settings;

public record TelegramAuthSettings(boolean enabled, String botToken, int loginExpireSeconds) {}
