package com.seohamin.galae.domain.user.service;

import com.seohamin.galae.domain.achievement.service.AchievementService;
import com.seohamin.galae.domain.user.dto.UserProfileResponseDto;
import com.seohamin.galae.domain.user.dto.UserRequestDto;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.util.PhotoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AchievementService achievementService;

    /**
     * 프로필 조회
     * @param targetUserId 볼 사람
     * @param viewerId 보는 사람 (같으면 내 프로필)
     */
    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile(final Long targetUserId, final Long viewerId) {
        final User user = findUser(targetUserId);
        return new UserProfileResponseDto(
                user,
                achievementService.getStats(targetUserId),
                achievementService.getAchievements(targetUserId),
                targetUserId.equals(viewerId)
        );
    }

    /**
     * 프로필 편집
     */
    @Transactional
    public UserProfileResponseDto updateProfile(final UserRequestDto userRequestDto, final Long userId) {
        final User user = findUser(userId);

        // 1) 아이디는 중복 확인 후 변경
        if (userRequestDto.getHandle() != null && !userRequestDto.getHandle().equals(user.getHandle())) {
            if (userRepository.existsByHandle(userRequestDto.getHandle())) {
                throw new CustomException(ExceptionCode.HANDLE_DUPLICATE);
            }
            user.updateHandle(userRequestDto.getHandle());
        }

        // 2) 나머지 정보
        Optional.ofNullable(userRequestDto.getName()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(user::updateName);
        Optional.ofNullable(userRequestDto.getBio()).map(String::trim).ifPresent(user::updateBio);
        Optional.ofNullable(userRequestDto.getColor()).ifPresent(user::updateColor);
        if (userRequestDto.getPhoto() != null) {
            user.updatePhoto(PhotoUtil.decode(userRequestDto.getPhoto()));
        }

        return getProfile(userId, userId);
    }

    /**
     * 프로필 사진 (img 태그로 불러오므로 인증 없이)
     */
    @Transactional(readOnly = true)
    public byte[] getPhoto(final Long userId) {
        final User user = findUser(userId);
        if (user.getPhoto() == null) {
            throw new CustomException(ExceptionCode.PHOTO_NOT_EXIST);
        }
        return user.getPhoto();
    }

    private User findUser(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
    }
}
