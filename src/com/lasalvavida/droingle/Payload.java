package com.lasalvavida.droingle;

import com.lasalvavida.droingle.Codec;

/**
 * A jingle payload type
 *
 * @author Rob Taglang
 */
public class Payload {

    private Codec codec;
    private int clockRate;
    private int id;

    public Payload(int id, Codec codec, int clockRate) {
        this.id = id;
        this.codec = codec;
        this.clockRate = clockRate;
    }

    public Payload(String id, String codec, String clockRate) {
        this.id = Integer.parseInt(id);
        this.codec = Codec.fromString(codec);
        this.clockRate = Integer.parseInt(clockRate);
    }

    public int getId() {
        return id;
    }

    public Codec getCodec() {
        return codec;
    }

    public int getClockRate() {
        return clockRate;
    }

    /**
     * @return an xml stanza describing the payload
     */
    public String toString() {
        return "<payload-type id='" + id + "' name='" + codec.toString() + "' clockrate='" + clockRate + "'/>";
    }
}
