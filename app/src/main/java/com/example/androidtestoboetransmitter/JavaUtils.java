package com.example.androidtestoboetransmitter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import kotlin.jvm.JvmStatic;

public class JavaUtils {

    @JvmStatic
    public static byte[] shortToByte_Twiddle_Method(short[] input) {
        int short_index, byte_index;
        int iterations = input.length;

        byte[] buffer = new byte[input.length * 2];

        short_index = byte_index = 0;

        for (/*NOP*/; short_index != iterations; /*NOP*/) {
            buffer[byte_index] = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

            ++short_index;
            byte_index += 2;
        }
        return buffer;
    }

    @JvmStatic
    public static short[] byteArrayToShortArray(byte[] bytes) {
//        byte[] bytes = {};
        short[] shorts = new short[bytes.length / 2];
// to turn bytes to shorts as either big endian or little endian.
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
// to turn shorts back to bytes.
//        byte[] bytes2 = new byte[shortsA.length * 2];
//        ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortsA);
    }
}
