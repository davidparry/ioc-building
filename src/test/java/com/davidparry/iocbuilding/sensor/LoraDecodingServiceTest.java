package com.davidparry.iocbuilding.sensor;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoraDecodingServiceTest {

    private final LoraDecodingService service = new LoraDecodingService();

    @Test
    void decodesTemperatureAndLeakDetection() {
        // channel 03 / type 67 = temperature 0x00FA * 0.1, channel 09 / type 00 = leak digital 0xFF
        byte[] payload = HexFormat.of().parseHex("036700FA0900FF");

        List<SensorReading> readings = service.decode("70B3D5CAFE000001", payload);

        assertThat(readings).hasSize(2);
        assertThat(readings.getFirst().type()).isEqualTo("T");
        assertThat(readings.getFirst().value()).isEqualTo(25.0);
        assertThat(readings.getLast().type()).isEqualTo("MD");
        assertThat(readings.getLast().value()).isEqualTo(1);
    }

    @Test
    void rejectsPayloadWithNoKnownChannels() {
        byte[] payload = HexFormat.of().parseHex("6364");

        assertThatThrownBy(() -> service.decode("70B3D5CAFE000001", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no readings");
    }
}
