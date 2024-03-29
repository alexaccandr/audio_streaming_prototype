package com.example.androidtestoboetransmitter;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * G.711 codec. This class provides methods for u-law, A-law and linear PCM
 * conversions.
 */
public class G711 {

    /*
 	 * conversion table alaw to linear
 	 */
    private static short [] a2l = {
            -5504, -5248, -6016, -5760, -4480, -4224, -4992, -4736,
            -7552, -7296, -8064, -7808, -6528, -6272, -7040, -6784,
            -2752, -2624, -3008, -2880, -2240, -2112, -2496, -2368,
            -3776, -3648, -4032, -3904, -3264, -3136, -3520, -3392,
            -22016, -20992, -24064, -23040, -17920, -16896, -19968, -18944,
            -30208, -29184, -32256, -31232, -26112, -25088, -28160, -27136,
            -11008, -10496, -12032, -11520, -8960, -8448, -9984, -9472,
            -15104, -14592, -16128, -15616, -13056, -12544, -14080, -13568,
            -344, -328, -376, -360, -280, -264, -312, -296,
            -472, -456, -504, -488, -408, -392, -440, -424,
            -88, -72, -120, -104, -24, -8, -56, -40,
            -216, -200, -248, -232, -152, -136, -184, -168,
            -1376, -1312, -1504, -1440, -1120, -1056, -1248, -1184,
            -1888, -1824, -2016, -1952, -1632, -1568, -1760, -1696,
            -688, -656, -752, -720, -560, -528, -624, -592,
            -944, -912, -1008, -976, -816, -784, -880, -848,
            5504, 5248, 6016, 5760, 4480, 4224, 4992, 4736,
            7552, 7296, 8064, 7808, 6528, 6272, 7040, 6784,
            2752, 2624, 3008, 2880, 2240, 2112, 2496, 2368,
            3776, 3648, 4032, 3904, 3264, 3136, 3520, 3392,
            22016, 20992, 24064, 23040, 17920, 16896, 19968, 18944,
            30208, 29184, 32256, 31232, 26112, 25088, 28160, 27136,
            11008, 10496, 12032, 11520, 8960, 8448, 9984, 9472,
            15104, 14592, 16128, 15616, 13056, 12544, 14080, 13568,
            344, 328, 376, 360, 280, 264, 312, 296,
            472, 456, 504, 488, 408, 392, 440, 424,
            88, 72, 120, 104, 24, 8, 56, 40,
            216, 200, 248, 232, 152, 136, 184, 168,
            1376, 1312, 1504, 1440, 1120, 1056, 1248, 1184,
            1888, 1824, 2016, 1952, 1632, 1568, 1760, 1696,
            688, 656, 752, 720, 560, 528, 624, 592,
            944, 912, 1008, 976, 816, 784, 880, 848
    };

    public static short alaw2linear( byte ulawbyte) {
        return a2l[ulawbyte & 0xFF];
    }

	/*
	 *
	 * linear2alaw() - Convert a 16-bit linear PCM value to 8-bit A-law
	 *
	 * linear2alaw() accepts an 16-bit integer and encodes it as A-law data.
	 *
	 *              Linear Input Code       Compressed Code
	 *      ------------------------        ---------------
	 *      0000000wxyza                    000wxyz
	 *      0000001wxyza                    001wxyz
	 *      000001wxyzab                    010wxyz
	 *      00001wxyzabc                    011wxyz
	 *      0001wxyzabcd                    100wxyz
	 *      001wxyzabcde                    101wxyz
	 *      01wxyzabcdef                    110wxyz
	 *      1wxyzabcdefg                    111wxyz
	 *
	 * For further information see John C. Bellamy's Digital Telephony, 1982,
	 * John Wiley & Sons, pps 98-111 and 472-476.
	 */

	private static final byte QUANT_MASK = 0xf; /* Quantization field mask. */
	private static final byte SEG_SHIFT = 4;  /* Left shift for segment number. */
	private static final short[] seg_end = {
			0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF
	};

	public static byte linear2alaw(short pcm_val) /* 2's complement (16-bit range) */ {
		byte mask;
		byte seg=8;
		byte aval;

		if (pcm_val >= 0) {
			mask = (byte) 0xD5;  /* sign (7th) bit = 1 */
		} else {
			mask = 0x55;  /* sign bit = 0 */
			pcm_val = (short) (-pcm_val - 8);
		}

		/* Convert the scaled magnitude to segment number. */
		for (int i = 0; i < 8; i++) {
			if (pcm_val <= seg_end[i]) {
				seg=(byte) i;
				break;
			}
		}

		/* Combine the sign, segment, and quantization bits. */
		if (seg >= 8)  /* out of range, return maximum value. */
			return (byte) ((0x7F ^ mask) & 0xFF);
		else {
			aval = (byte) (seg << SEG_SHIFT);
			if (seg < 2)
				aval |= (pcm_val >> 4) & QUANT_MASK;
			else
				aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
			return (byte) ((aval ^ mask) & 0xFF);
		}
	}

	public static byte[] linearArray2alawArray(byte[] bytes) {
		short[] shorts = new short[bytes.length/2];
		ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).asShortBuffer().get(shorts);
		byte[] data = new byte[shorts.length];

		for (int j = 0; j < shorts.length; j++) {
			data[j] = linear2alaw(shorts[j]);
		}

		return data;
	}
}
