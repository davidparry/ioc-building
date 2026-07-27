package com.davidparry.iocbuilding.sensor;

import com.davidparry.lora.codec.Sensor;

/**
 * One decoded measurement from an uplink payload, e.g. type=T label=Temperature value=25.0.
 */
public record SensorReading(String type, String label, Object value) {

    public static SensorReading from(Sensor<?> sensor) {
        return new SensorReading(
                sensor.getDataType().getKey().name(),
                sensor.getDataType().getLabel(),
                sensor.getValue());
    }
}
