# Weather App

A server-side rendered weather application built with Spring Boot 4 that provides current conditions, hourly forecasts, and a 7-day outlook for any city worldwide. All data is sourced from the free [Open-Meteo](https://open-meteo.com/) APIs — no API key required.

---

## Features

- **City autocomplete** — real-time city search powered by the Open-Meteo Geocoding API
- **Current conditions** — temperature, feels-like, humidity, wind, pressure, UV index, visibility, cloud cover
- **Hourly forecast** — next 24 hours with temperature and precipitation probability, interactive chart
- **7-day daily forecast** — min/max temps, precipitation, wind, sunrise/sunset, UV index
- **Smart caching** — weather data cached for 10 minutes, geocoding results for 24 hours (Caffeine)
- **No authentication** — all endpoints are public

---

## Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.x (`spring-boot-starter-webmvc`) |
| Templating | Thymeleaf |
| HTTP client | Java `HttpClient` (JDK 11+) |
| JSON | Jackson 3 (tools.jackson) |
| Cache | Spring Cache + Caffeine |
| Monitoring | Spring Boot Actuator (`/actuator/health`) |
| Utilities | Lombok |
| Build | Maven |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/weather/app/
│   │   ├── WeatherApplication.java       # Entry point
│   │   ├── config/
│   │   │   ├── CacheConfig.java          # Caffeine cache definitions (weather 10 min, geocoding 24 h)
│   │   │   └── HttpClientConfig.java     # Shared HttpClient bean (TLS trust-all)
│   │   ├── controller/
│   │   │   └── WeatherController.java    # GET /, GET /weather, GET /api/cities
│   │   ├── model/
│   │   │   ├── GeoLocation.java          # Geocoding result entity
│   │   │   ├── GeocodingResponse.java    # Open-Meteo geocoding API response wrapper
│   │   │   ├── WeatherData.java          # Open-Meteo forecast API response
│   │   │   └── WeatherViewModel.java     # View model passed to Thymeleaf templates
│   │   └── service/
│   │       ├── GeocodingService.java     # City search and lookup (cached)
│   │       ├── WeatherCodeService.java   # WMO weather code → description / icon / CSS class
│   │       └── WeatherService.java       # Forecast fetch + ViewModel builder (cached)
│   └── resources/
│       ├── application.yml
│       ├── static/css/style.css
│       └── templates/
│           ├── index.html                # Search page
│           └── weather.html             # Weather detail page
└── test/
    └── java/com/weather/app/
        ├── config/
        │   └── CacheIntegrationTest.java # Cache hit/miss integration tests
        ├── controller/
        │   └── WeatherControllerTest.java
        ├── model/
        │   └── GeoLocationTest.java
        └── service/
            ├── GeocodingServiceTest.java
            ├── WeatherCodeServiceTest.java
            └── WeatherServiceTest.java
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Search page (Thymeleaf view) |
| `GET` | `/weather?city=Roma[&lat=41.89&lon=12.48]` | Weather detail page |
| `GET` | `/api/cities?q=Roma` | City autocomplete (JSON array) |
| `GET` | `/actuator/health` | Health check |

---

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+

### Steps

```bash
# Clone the repo
git clone <repo-url>
cd weather-app

# Build
mvn clean package -P local

# Run
java -jar target/weather-app.jar
```

The application starts on **http://localhost:8080** by default.
Override the port with the `PORT` environment variable:

```bash
PORT=9090 java -jar target/weather-app.jar
```

### Dev mode (with Spring DevTools)

```bash
mvn spring-boot:run -P local
```

---

## Running with Docker

### Prerequisites

- Docker 20.10+

### Build the image

```bash
docker build -t weather-app .
```

The Dockerfile uses a **multi-stage build**:
1. **Build stage** — `maven:3.9-eclipse-temurin-21` compiles the project and produces the JAR (tests skipped)
2. **Runtime stage** — `eclipse-temurin:21-jre-alpine` runs the JAR as a non-root user (`appuser`)

### Run the container

```bash
docker run -p 8080:8080 weather-app
```

Override the port (maps container port 8080 to host port 9090):

```bash
docker run -p 9090:8080 -e PORT=9090 weather-app
```

The container exposes a health check on `/actuator/health` with the following parameters:

| Parameter | Value |
|---|---|
| Interval | 30s |
| Timeout | 5s |
| Start period | 30s |
| Retries | 3 |

Check container health status:

```bash
docker ps   # look at the STATUS column
```

---

## Running Tests

```bash
# All tests
mvn clean test

# Single class
mvn test -Dtest=WeatherServiceTest
```

> **Note:** `spring-boot-starter-security` is a `test`-scope dependency only — it is required because `spring-boot-starter-actuator` (Spring Boot 4.x) transitively references `SecurityAutoConfiguration`. The application itself has no security configuration and all endpoints are public.

---

## Configuration

All properties live in `src/main/resources/application.yml`.

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port (overridable via `PORT` env var) |
| `weather.forecast.url` | `https://api.open-meteo.com/v1` | Open-Meteo forecast base URL |
| `weather.geocoding.url` | `https://geocoding-api.open-meteo.com/v1` | Open-Meteo geocoding base URL |
