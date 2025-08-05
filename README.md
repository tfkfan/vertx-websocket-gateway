<h4 align="center">
This is vert.x based distributed java stomp websocket gateway server with kafka integration</h4>

---

Features:

* Websocket
* STOMP
* Apache kafka (for interservice communication)
* Vert.X cluster
* Monitoring (micrometer, prometheus)
* Changelog.md generation (for cicd/release process)
* Conventional commits

## Usage

The server sources already contain static html websocket resources demo available at http://localhost:8080.
Run all required containers:

```shell
docker compose -f ./docker/all.yml up -d
```

Then run java app. You can produce message to websocket_gateway_output_topic using redpanda console which available at http://localhost:8085

Example of output message:

```json
{
  "sender": "sender sample",
  "stompChannel": "/example_output",
  "message": "message sample",
  "createdAt": "...",
  "recipient": "0a32e82b-45e9-4250-b430-e4acb4980746"
}
```

Example of input (ws) message:

```json
{
  "sender": "sender sample",
  "message": "message sample",
  "createdAt": "..."
}
```

Notice, input messages are not conceived to be responded

Recipient is UUID ws session indentifier printed in server console when new client is connected

## Metrics

Prometheus metrics are enabled by default and available at /prometheus endpoint
JSON-encoded metrics are available at /metrics

## Requirements

### Kafka topic retention
All kafka topic used by websocket gateway should be adjusted with low retention.ms. One minute is enough

### Environment variables
Following variables should be provided:
- KAFKA_BOOTSTRAPSERVERS - optional, required for cloud setup
- APP_INPUT_STOMP_KAFKA_MAPPING - required for stomp-kafka input mapping. Format: "/stomp_channel1:kafka_topic1","/stomp_channel2:kafka_topic2"
