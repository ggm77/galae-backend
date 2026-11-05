package com.seohamin.galae.global.util;

import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 질문 이미지처럼 JPEG 말고 PNG·WebP도 받는 이미지(data URL)를 바이트로 바꾼다
 * 형식은 data URL의 타입과 파일 머리(시그니처)를 둘 다 확인한다
 */
public final class ImageUtil {

    private ImageUtil() {}

    private static final Pattern DATA_URL = Pattern.compile("^data:(image/(?:jpeg|png|webp));base64,(.+)$", Pattern.DOTALL);

    public record DecodedImage(byte[] bytes, String contentType) {}

    /**
     * @param dataUrl  data:image/jpeg|png|webp;base64,...
     * @param maxBytes 디코딩한 뒤의 최대 크기
     */
    public static DecodedImage decode(final String dataUrl, final int maxBytes) {
        final Matcher m = DATA_URL.matcher(dataUrl);
        if (!m.matches()) {
            throw new CustomException(ExceptionCode.INVALID_PHOTO, "이미지는 JPEG, PNG, WebP만 올릴 수 있습니다.");
        }

        final byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(m.group(2));
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionCode.INVALID_PHOTO);
        }

        if (bytes.length > maxBytes) {
            throw new CustomException(ExceptionCode.TOO_BIG_PHOTO, "이미지는 " + (maxBytes / 1024) + "KB 이하여야 합니다.");
        }

        final String contentType = m.group(1);
        if (!signatureMatches(bytes, contentType)) {
            throw new CustomException(ExceptionCode.INVALID_PHOTO);
        }
        return new DecodedImage(bytes, contentType);
    }

    private static boolean signatureMatches(final byte[] b, final String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> b.length >= 3 && u(b[0]) == 0xFF && u(b[1]) == 0xD8 && u(b[2]) == 0xFF;
            case "image/png" -> b.length >= 8 && u(b[0]) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                    && u(b[4]) == 0x0D && u(b[5]) == 0x0A && u(b[6]) == 0x1A && u(b[7]) == 0x0A;
            case "image/webp" -> b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }

    private static int u(final byte value) {
        return value & 0xFF;
    }
}
