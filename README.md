# ioc-building

Spring Boot microservice for a building IoT system. It ingests LoRaWAN sensor
uplinks (Tektelic Home sensors: leak/moisture, temperature, humidity, etc.) and
decodes the raw payloads with the
[lora-codecs](https://github.com/davidparry/lora-codecs) library.

This service is the target repository for the `code-review` agent platform:
new features arrive as requirement-tool webhooks and bug reports arrive from a
log-monitor microservice that tails this service's logs.

## API

```
POST /api/v1/uplink
{"devEui": "70B3D5CAFE000001", "payloadHex": "036700FA0900FF"}
```

Returns the decoded readings (`payloadBase64` is accepted as an alternative to
`payloadHex`). A moisture/leak reading of `1` logs `LEAK DETECTED`; undecodable
payloads are logged as errors with a stack trace, which the log-monitor
microservice turns into bug reports for the code-review agent.

## Building

The `lora-codecs` dependency is compiled from source into `libs/` because the
old OSS Sonatype snapshots repository is retired and its Gradle 5.1 build does
not run on modern JDKs:

```bash
./scripts/build-lora-codecs.sh   # expects ../lora-codecs (or set LORA_CODECS_DIR)
./gradlew build
```

## Docker

```bash
docker build --build-context lora-codecs=../lora-codecs -t ioc-building .
docker run -p 8091:8080 ioc-building
```

The compose stack in `embabel-req-to-code/infra/compose.yaml` builds this image
with the `lora-codecs` context wired up, writes logs to a shared volume, and
runs the log-monitor microservice against them.
