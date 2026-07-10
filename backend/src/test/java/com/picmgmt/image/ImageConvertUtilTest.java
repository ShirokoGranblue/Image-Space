package com.picmgmt.image;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageConvertUtilTest {

    @Test
    void rejectsPngWhoseDecodedPixelCountExceedsBudget() throws Exception {
        byte[] oversizedHeader = pngHeader(50_000, 50_000);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ImageConvertUtil.readSupportedImage(oversizedHeader, "image/png", "png")
        );

        assertEquals(ErrorCode.IMAGE_SIZE_EXCEEDED, exception.getErrorCode());
    }

    private byte[] pngHeader(int width, int height) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        output.writeInt(13);
        byte[] type = new byte[]{0x49, 0x48, 0x44, 0x52};
        ByteArrayOutputStream chunkBytes = new ByteArrayOutputStream();
        DataOutputStream chunk = new DataOutputStream(chunkBytes);
        chunk.write(type);
        chunk.writeInt(width);
        chunk.writeInt(height);
        chunk.writeByte(8);
        chunk.writeByte(2);
        chunk.writeByte(0);
        chunk.writeByte(0);
        chunk.writeByte(0);
        byte[] chunkData = chunkBytes.toByteArray();
        output.write(chunkData);
        CRC32 crc = new CRC32();
        crc.update(chunkData);
        output.writeInt((int) crc.getValue());
        return bytes.toByteArray();
    }
}
