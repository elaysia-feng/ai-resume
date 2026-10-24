package com.elias.chat.tool;

/**
 * 二进制文件探测。
 *
 * <p>把乱码塞给模型既浪费 token 又会污染上下文，
 * 读文件和搜索内容时都要先判断是不是文本文件。</p>
 */
public final class BinaryDetector {

    /**
     * 采样字节数
     */
    private static final int SAMPLE_BYTES = 8192;

    /**
     * 控制字符占比阈值
     */
    private static final double CONTROL_CHAR_RATIO = 0.1;

    private BinaryDetector() {
    }

    /**
     * 是否是二进制文件
     */
    public static boolean isBinary(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        int sample = Math.min(data.length, SAMPLE_BYTES);
        int controlCount = 0;
        for (int i = 0; i < sample; i++) {
            int value = data[i] & 0xFF;
            if (value == 0) {
                return true;
            }
            if (value < 0x09 || (value > 0x0D && value < 0x20)) {
                controlCount++;
            }
        }
        return (double) controlCount / sample > CONTROL_CHAR_RATIO;
    }
}
