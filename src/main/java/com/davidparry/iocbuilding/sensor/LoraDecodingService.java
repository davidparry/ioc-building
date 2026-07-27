package com.davidparry.iocbuilding.sensor;

import com.davidparry.lora.codec.Codec;
import com.davidparry.lora.codec.Payload;
import com.davidparry.lora.codec.Sensor;
import com.davidparry.lora.codec.SensorType;
import com.davidparry.lora.codec.internal.ByteArrayPayloadReaderReader;
import com.davidparry.lora.codec.tektelic.TektelicHomeCodec;
import com.davidparry.lora.codec.tektelic.TektelicHomeRegisterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Decodes raw LoRaWAN payloads with the lora-codecs library (Tektelic Home
 * sensors) and flags leak detections.
 */
@Service
public class LoraDecodingService {

    private static final Logger log = LoggerFactory.getLogger(LoraDecodingService.class);

    public List<SensorReading> decode(String devEui, byte[] payload) {
        // The payload reader is stateful, so a fresh codec is built per uplink.
        Codec<byte[]> codec = new TektelicHomeCodec(new TektelicHomeRegisterImpl(), new ByteArrayPayloadReaderReader());
        Payload decoded = codec.decode(payload);
        List<SensorReading> readings = decoded.getSensors().stream()
                .map(sensor -> toReading(devEui, sensor))
                .toList();
        if (readings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Payload from device " + devEui + " produced no readings (unknown channels)");
        }
        return readings;
    }

    private SensorReading toReading(String devEui, Sensor<?> sensor) {
        SensorReading reading = SensorReading.from(sensor);
        if (sensor.getDataType().getKey() == SensorType.MD && isTriggered(reading.value())) {
            log.warn("LEAK DETECTED by device {} ({})", devEui, sensor.getDataType().getLabel());
        }
        return reading;
    }

    private boolean isTriggered(Object value) {
        return value instanceof Number number && number.intValue() != 0;
    }
}
