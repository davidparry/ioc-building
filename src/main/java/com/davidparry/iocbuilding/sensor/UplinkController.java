package com.davidparry.iocbuilding.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Ingestion endpoint for LoRaWAN uplinks forwarded by the network server.
 */
@RestController
@RequestMapping("/api/v1/uplink")
public class UplinkController {

    private static final Logger log = LoggerFactory.getLogger(UplinkController.class);

    private final LoraDecodingService decodingService;

    public UplinkController(LoraDecodingService decodingService) {
        this.decodingService = decodingService;
    }

    @PostMapping
    public UplinkResponse ingest(@RequestBody UplinkRequest request) {
        byte[] payload = request.payloadBytes();
        List<SensorReading> readings = decodingService.decode(request.devEui(), payload);
        log.info("Uplink from {}: {} readings {}", request.devEui(), readings.size(), readings);
        return new UplinkResponse(request.devEui(), readings, Instant.now());
    }

    /**
     * Malformed or undecodable payloads are logged with a stack trace so the
     * log-monitor microservice can raise a bug report to the code-review agent.
     */
    @ExceptionHandler({IllegalArgumentException.class, RuntimeException.class})
    public ResponseEntity<Map<String, String>> onDecodeFailure(RuntimeException e) {
        log.error("Failed to decode uplink payload", e);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage())));
    }
}
