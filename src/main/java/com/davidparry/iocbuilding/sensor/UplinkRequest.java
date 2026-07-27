package com.davidparry.iocbuilding.sensor;

import java.util.Base64;
import java.util.HexFormat;

/**
 * A LoRaWAN uplink as forwarded by a network server. The raw payload may be
 * supplied as hex ({@code payloadHex}) or base64 ({@code payloadBase64}).
 */
public record UplinkRequest(
        String devEui,
        String payloadHex,
        String payloadBase64
) {

    public byte[] payloadBytes() {
        if (payloadHex != null && !payloadHex.isBlank()) {
            return HexFormat.of().parseHex(payloadHex.trim());
        }
        if (payloadBase64 != null && !payloadBase64.isBlank()) {
            return Base64.getDecoder().decode(payloadBase64.trim());
        }
        throw new IllegalArgumentException("Uplink must carry payloadHex or payloadBase64");
    }
}
