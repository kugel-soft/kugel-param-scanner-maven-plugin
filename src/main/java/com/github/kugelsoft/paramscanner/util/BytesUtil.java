package com.github.kugelsoft.paramscanner.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BytesUtil {

    public static final byte[] EMPTY_ARRAY = new byte[0];

    private BytesUtil() {
    }

    public static byte[] readAllBytes(InputStream inputStream) {
        try {
            final int bufLen = 10240;
            byte[] buf = new byte[bufLen];
            int readLen;
            IOException exception = null;

            try {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                        outputStream.write(buf, 0, readLen);

                    return outputStream.toByteArray();
                }
            } catch (IOException e) {
                exception = e;
                throw e;
            } finally {
                if (exception == null)
                    inputStream.close();
                else
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        exception.addSuppressed(e);
                    }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}
