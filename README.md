# Location Service

## Develop branch status

**Repository skeleton; not part of the runtime catalogue or Docker Compose.** The implemented location path is Client BFF → Location Gateway → Postcode.io Gateway. The text below describes intended commute-domain architecture; a callable API, persistence model, and Google Maps call path are not present on `develop`.

See the central [location journey](https://docs.jobseekercopilot.com/journeys/location/) and [implementation status](https://docs.jobseekercopilot.com/reference/implementation-status/).

Provider-neutral location normalisation and commute orchestration for Job Seeker
Copilot.

```text
location-gateway ─┐
                  ├─> location-service ─> google-maps-gateway
job-matching ─────┘                    └─> postcode-io-gateway
```

The Angular client never calls this service directly. `location-gateway` owns
the public candidate API. This service owns canonical location rules, provider
selection, suggestion-session lifecycle and the transient commute matrix
capability used by matching. Google DTOs and credentials remain in
`google-maps-gateway`.

Google-assisted behavior is disabled by default. When disabled, autocomplete
uses the approved Postcodes.io fallback and commute routing returns a typed
unavailable result to its advisory consumer.

## Requirements and configuration

- Java 17 and Maven 3.9
- `postcode-io-gateway`
- optionally `google-maps-gateway`

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8104` | HTTP port |
| `LOCATION_SERVICE_TOKEN` | none, required | Internal caller authentication; at least 32 bytes |
| `POSTCODE_IO_GATEWAY_URL` | `http://localhost:8082` | Postcodes.io gateway |
| `GOOGLE_MAPS_GATEWAY_URL` | `http://localhost:8105` | Google provider gateway |
| `GOOGLE_MAPS_GATEWAY_TOKEN` | none, required | Google gateway caller token; at least 32 bytes |
| `GOOGLE_MAPS_ENABLED` | `false` | Explicit paid-provider enablement |
| `LOCATION_SESSION_TTL` | `10m` | Opaque suggestion-session lifetime |
| `LOCATION_SESSION_MAXIMUM_ENTRIES` | `10000` | Bounded in-memory sessions |
| `LOCATION_SEARCH_MAXIMUM_RESULTS` | `5` | Maximum autocomplete suggestions |
| `LOCATION_COMMUTE_MAXIMUM_DESTINATIONS` | `5` | Maximum destinations per route request |

`GOOGLE_MAPS_API_KEY` is not accepted or used by this service.

## API and contract

The producer-owned internal OpenAPI 1.0.0 source is
[`api/openapi.yaml`](api/openapi.yaml). Consumers check in the exact reviewed
snapshot and generate clients into disposable Maven build output.

- `POST /internal/v1/locations/autocomplete`
- `POST /internal/v1/locations/resolve`
- `GET /internal/v1/postcodes/{postcode}`
- `POST /internal/v1/commutes/matrix`
- `/actuator/health`
- `/actuator/health/readiness`

Every application endpoint requires `X-Service-Token`; health endpoints do not.
Google suggestion text and coordinates are transient. Persistable coordinates
and labels are sourced from Postcodes.io/legacy centroids, and commute results
are never persisted here.

## Build

```bash
LOCATION_SERVICE_TOKEN=test-only-location-service-token-32-bytes \
GOOGLE_MAPS_GATEWAY_TOKEN=test-only-google-maps-gateway-token-32-bytes \
GOOGLE_MAPS_ENABLED=false \
mvn -B clean verify
```

Normal tests and CI do not contact Google or any paid provider. Generated source,
JARs, provider payloads, environment files and credentials are not committed.

## Branch workflow

Use `feature/* → develop`. Do not merge application delivery directly into
`main`.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).
