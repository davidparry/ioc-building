package com.davidparry.iocbuilding.sensor;

import java.time.Instant;
import java.util.List;

public record UplinkResponse(
        String devEui,
        List<SensorReading> readings,
        Instant receivedAt
) {
}
