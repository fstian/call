package com.androidport.port.audio;

public class G711 {
    private static int SIGN_BIT = (0x80);        /* Sign bit for a A-law byte. */
    private static int QUANT_MASK = (0xf);    /* Quantization field mask. */
    private int NSEGS = (8);            /* Number of A-law segments. */
    private static int SEG_SHIFT = (4);        /* Left shift for segment number. */
    private static int SEG_MASK = (0x70);        /* Segment field mask. */
    private static int BIAS = (0x84);
    private static short[] seg_end = {0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF};


    static short alaw2linear(byte a_val) {
        int t;
        int seg;

        a_val ^= 0x55;

        t = ((a_val & QUANT_MASK) << 4);
        seg = ((a_val & SEG_MASK) >> SEG_SHIFT);
        switch (seg) {
            case 0:
                t += 8;
                break;
            case 1:
                t += 0x108;
                break;
            default:
                t += 0x108;
                t <<= seg - 1;
        }
        return (a_val & SIGN_BIT) != 0 ? (short) t : (short) -t;
    }

    static short ulaw2linear(byte u_val) {
        short t;

        /* Complement to obtain normal u-law value. */
        u_val = (byte) ~u_val;

        /*
         * Extract and bias the quantization bits. Then
         * shift up by the segment number and subtract out the bias.
         */
        t = (short) (((u_val & QUANT_MASK) << 3) + BIAS);
        t <<= (u_val & SEG_MASK) >> SEG_SHIFT;
        return (short) ((u_val & SIGN_BIT) == SIGN_BIT ? (BIAS - t) : (t - BIAS));

    }




    static byte linear2alaw(short pcm_val) {
        short mask;
        int seg;
        char aval;
        if (pcm_val >= 0) {
            mask = 0xD5;
        } else {
            mask = 0x55;
            pcm_val = (short) (-pcm_val - 1);
            if (pcm_val < 0) {
                pcm_val = 32767;
            }
        }
        /* Convert the scaled magnitude to segment number. */
        seg = search(pcm_val, seg_end, (short) 8);
        /* Combine the sign, segment, and quantization bits. */
        if (seg >= 8) {/* out of range, return maximum value. */
            return (byte) (0x7F ^ mask);

        } else {
            aval = (char) (seg << SEG_SHIFT);
            if (seg < 2) {
                aval |= (pcm_val >> 4) & QUANT_MASK;
            } else {
                aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
            }
            return (byte) (aval ^ mask);
        }
    }

    
    private static int search(int val, short[] table, int size) {
        int i;

        for (i = 0; i < size; i++) {
            if (val <= table[i]) {
                return (i);
            }
        }
        return (size);
    }

    static byte linear2ulaw(short pcm_val) /* 2's complement (16-bit range) */ {
        int mask;
        int seg;
        byte uval;

        /* Get the sign and the magnitude of the value. */
        if (pcm_val < 0) {
            pcm_val = (short) (BIAS - pcm_val);
            mask = 0x7F;
        } else {
            pcm_val += BIAS;
            mask = 0xFF;
        }

        /* Convert the scaled magnitude to segment number. */
        seg = search(pcm_val, seg_end, 8);
        /*
         * Combine the sign, segment, quantization bits;
         * and complement the code word.
         */
        if (seg >= 8) {/* out of range, return maximum value. */
            return (byte) (0x7F ^ mask);

        } else {
            uval = (byte) ((seg << 4) | ((pcm_val >> (seg + 3)) & 0xF));
            return (byte) (uval ^ mask);
        }
    }

}
