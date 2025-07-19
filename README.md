<h4 align="center">
This is vert.x based distributed java stomp websocket gateway server with kafka integration</h4>

---

Features:

* Websocket
* STOMP
* Apache kafka (for interservice communication)
* Vert.X cluster

## Usage

The server sources already contain static html websocket resources demo available at http://localhost:8080.
Run all required containers:

```shell
docker compose -f ./docker/all.yml up -d
```

Then run java app. You can produce message to websocket_gateway_output_topic using redpanda console which available at http://localhost:8085

Example of message:

```json
{
  "sender": "sender sample",
  "stompChannel": "/example_output",
  "message": "message sample",
  "createdAt": "...",
  "recipient": "0a32e82b-45e9-4250-b430-e4acb4980746"
}
```

Recipient is UUID ws session indentifier printed in server console when new client is connected

## Requirements

All kafka topic used by websocket gateway should be adjusted with low retention.ms. One minute is enough