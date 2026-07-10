package com.picmgmt.image;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Iterator;

public final class ImageConvertUtil {

    static final int MAX_IMAGE_DIMENSION = 16_384;
    static final long MAX_IMAGE_PIXELS = 40_000_000L;

    private ImageConvertUtil() {
    }

    public static byte[] convertToPng(byte[] sourceBytes) {
        BufferedImage image = readSupportedImage(sourceBytes, null, null);
        return writePng(image);
    }

    public static byte[] convertToJpg(byte[] sourceBytes) {
        BufferedImage image = readSupportedImage(sourceBytes, null, null);
        return writeJpg(image);
    }

    public static byte[] extractGifFirstFrameToPng(byte[] gifBytes) {
        // GIF to PNG exports only the first frame; the original GIF is never rewritten.
        BufferedImage image = readSupportedImage(gifBytes, "image/gif", "gif");
        return writePng(image);
    }

    public static byte[] extractGifFirstFrameToJpg(byte[] gifBytes) {
        // GIF to JPG exports only the first frame; the original GIF animation remains untouched.
        BufferedImage image = readSupportedImage(gifBytes, "image/gif", "gif");
        return writeJpg(image);
    }

    public static ImageVariant createDisplayVariant(byte[] sourceBytes, String contentType, String ext, int maxWidth) {
        if (isWebp(contentType, ext, sourceBytes)) {
            return null;
        }
        BufferedImage source = readSupportedImage(sourceBytes, contentType, ext);
        BufferedImage scaled = scaleToMaxWidth(source, maxWidth);
        boolean alpha = hasAlpha(scaled);
        String targetExt = alpha ? "png" : "jpg";
        byte[] bytes = alpha ? writePng(scaled) : writeJpg(scaled);
        return new ImageVariant(bytes, targetExt, contentTypeForExt(targetExt), scaled.getWidth(), scaled.getHeight());
    }

    public static BufferedImage readSupportedImage(byte[] sourceBytes, String contentType, String ext) {
        if (isWebp(contentType, ext, sourceBytes)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前环境不支持 WebP 格式转换");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(sourceBytes))) {
            if (input == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件解析失败");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件解析失败");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件解析失败");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件解析失败");
        }
    }

    private static void validateDimensions(int width, int height) {
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0
                || width > MAX_IMAGE_DIMENSION
                || height > MAX_IMAGE_DIMENSION
                || pixels > MAX_IMAGE_PIXELS) {
            throw new BusinessException(
                    ErrorCode.IMAGE_SIZE_EXCEEDED,
                    "图片尺寸过大，最大允许 16384×16384 且总像素不超过 4000 万"
            );
        }
    }

    public static boolean isGif(String contentType, String ext, byte[] bytes) {
        if ("image/gif".equalsIgnoreCase(nullToBlank(contentType))) {
            return true;
        }
        if ("gif".equals(normalizeExt(ext))) {
            return true;
        }
        return bytes != null && bytes.length >= 6
                && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
    }

    public static boolean isWebp(String contentType, String ext, byte[] bytes) {
        if ("image/webp".equalsIgnoreCase(nullToBlank(contentType))) {
            return true;
        }
        if ("webp".equals(normalizeExt(ext))) {
            return true;
        }
        return bytes != null && bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    public static boolean hasAlpha(BufferedImage image) {
        return image != null && image.getColorModel().hasAlpha();
    }

    public static String contentTypeForExt(String ext) {
        return switch (normalizeExt(ext)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private static BufferedImage scaleToMaxWidth(BufferedImage source, int maxWidth) {
        if (source.getWidth() <= maxWidth) {
            return source;
        }
        int targetWidth = maxWidth;
        int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * (targetWidth / (double) source.getWidth())));
        int type = hasAlpha(source) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, type);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!hasAlpha(source)) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, targetWidth, targetHeight);
            }
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] writePng(BufferedImage image) {
        return write(image, "png");
    }

    private static byte[] writeJpg(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return write(rgb, "jpg");
    }

    private static byte[] write(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean written = ImageIO.write(image, format, out);
            if (!written) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件解析失败");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件解析失败");
        }
    }

    private static String normalizeExt(String ext) {
        String value = nullToBlank(ext).trim().toLowerCase(Locale.ROOT);
        return "jpeg".equals(value) ? "jpg" : value;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
