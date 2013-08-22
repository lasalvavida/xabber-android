package com.lasalvavida.droingle;

import android.media.AudioFormat;

/**
 * A codec for jingle
 *
 * @author Rob Taglang
 */
public enum Codec {
    speex,
    G729,
    PCMU,
    L16,
    x_ISAC;

    public static Codec fromString(String name) {
        for(Codec c : values()) {
            if(c.toString().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public int toAndroid() {
        if(this == Codec.L16) {
            return AudioFormat.ENCODING_PCM_16BIT;
        }
        return -1;
    }
}
