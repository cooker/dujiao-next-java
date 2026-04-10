package com.dujiao.api.service;

import com.dujiao.api.auth.AuthConstants;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.dto.user.ChangePasswordRequest;
import com.dujiao.api.dto.user.UpdateProfileRequest;
import com.dujiao.api.dto.user.UserProfileDto;
import com.dujiao.api.repository.UserAccountRepository;
import com.dujiao.api.telegram.TelegramIdentityHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static String resolveEmailChangeMode(UserAccount u) {
        if (u == null) {
            return AuthConstants.EMAIL_CHANGE_MODE_CHANGE_WITH_OLD_AND_NEW;
        }
        if (TelegramIdentityHelper.isPlaceholderEmail(u.getEmail())) {
            return AuthConstants.EMAIL_CHANGE_MODE_BIND_ONLY;
        }
        return AuthConstants.EMAIL_CHANGE_MODE_CHANGE_WITH_OLD_AND_NEW;
    }

    public static String resolvePasswordChangeMode(UserAccount u) {
        if (u == null) {
            return AuthConstants.PASSWORD_CHANGE_MODE_CHANGE_WITH_OLD;
        }
        if (u.isPasswordSetupRequired()) {
            return AuthConstants.PASSWORD_CHANGE_MODE_SET_WITHOUT_OLD;
        }
        return AuthConstants.PASSWORD_CHANGE_MODE_CHANGE_WITH_OLD;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUser(long userId) {
        UserAccount u =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        return toProfileDto(u);
    }

    private static UserProfileDto toProfileDto(UserAccount u) {
        return new UserProfileDto(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getStatus(),
                u.getMemberLevelId(),
                resolveEmailChangeMode(u),
                resolvePasswordChangeMode(u));
    }

    @Transactional
    public UserProfileDto updateProfile(long userId, UpdateProfileRequest req) {
        UserAccount u =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (req.displayName() != null && !req.displayName().isBlank()) {
            u.setDisplayName(req.displayName().trim());
        }
        userAccountRepository.save(u);
        return getUser(userId);
    }

    @Transactional
    public void changePassword(long userId, ChangePasswordRequest req) {
        UserAccount u =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (u.isPasswordSetupRequired()) {
            if (req.oldPassword() != null && !req.oldPassword().isBlank()) {
                if (!passwordEncoder.matches(req.oldPassword(), u.getPasswordHash())) {
                    throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_old_password");
                }
            }
        } else {
            if (req.oldPassword() == null || req.oldPassword().isBlank()) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_old_password");
            }
            if (!passwordEncoder.matches(req.oldPassword(), u.getPasswordHash())) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_old_password");
            }
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        u.setPasswordSetupRequired(false);
        userAccountRepository.save(u);
    }
}
