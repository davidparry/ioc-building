# ioc-building

Mono repo for the building IoT system, target of the `code-review` agent
platform: new features arrive as requirement-tool webhooks and bug reports
arrive from the log monitor. It contains two microservices, each with its own
Docker image:

- **ioc-building** (repo root) — Spring Boot service that ingests LoRaWAN
  sensor uplinks (Tektelic Home sensors: leak/moisture, temperature, humidity,
  etc.) and decodes the raw payloads with the
  [lora-codecs](https://github.com/davidparry/lora-codecs) library.
- **log-monitor** (`log-monitor/`) — Python service that tails the
  ioc-building log file from a shared volume and posts bug reports to the
  code-review agent's `/webhook/bugreport` endpoint.

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

docker build -t log-monitor ./log-monitor
```

The compose stack in `embabel-req-to-code/infra/compose.yaml` builds both
images, shares the log directory between them on a volume, and points the
log monitor at the code-review agent.
