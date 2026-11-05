package com.seohamin.galae.global.util;

import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;

import java.util.Base64;

/**
 * 프론트가 보내는 사진(data:image/jpeg;base64,...)을 바이트로 바꾼다.
 * 프론트는 256px로 줄여 보내므로 크기 제한을 작게 둔다.
 */
public final class PhotoUtil {

    private PhotoUtil() {}

    private static final String PREFIX = "data:image/jpeg;base64,";
    private static final int MAX_BYTES = 300 * 1024;

    /**
     * @param dataUrl data URL. 빈 문자열이면 사진 삭제라서 null을 리턴한다
     * @return JPEG 바이트 또는 null
     */
    public static byte[] decode(final String dataUrl) {
        if (dataUrl.isEmpty()) {
            return null;
        }
        if (!dataUrl.startsWith(PREFIX)) {
            throw new CustomException(ExceptionCode.INVALID_PHOTO);
        }

        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionCode.INVALID_PHOTO);
        }

        if (bytes.length > MAX_BYTES) {
            throw new CustomException(ExceptionCode.TOO_BIG_PHOTO);
        }
        //JPEG 시그니처 확인 (FF D8 FF)
        if (bytes.length < 3 || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8 || (bytes[2] & 0xFF) != 0xFF) {
            throw new CustomException(ExceptionCode.INVALID_PHOTO);
        }
        return bytes;
    }
}
