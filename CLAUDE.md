# CLAUDE.md — Weather App

## Project Overview

Spring Boot 4.x server-side rendered weather application using Java 21.
All endpoints are **public** — no authentication, no authorization.
Data sources: Open-Meteo forecast API + Open-Meteo geocoding API (both free, no API key).

## Build & Test Commands

```bash
# Full build
mvn clean install

# Run tests only
mvn clean test

# Package JAR
mvn clean package

# Run locally (with DevTools)
mvn spring-boot:run -P local

# Run JAR
java -jar target/weather-app.jar
```

## Project Structure

```
src/main/java/com/weather/app/
├── WeatherApplication.java
├── config/
│   ├── CacheConfig.java          # Caffeine cache: "weather" (10 min), "geocoding" (24 h)
│   └── HttpClientConfig.java     # Bean: "unTrustedHttpClient" (TLS trust-all)
├── controller/
│   └── WeatherController.java    # GET /, GET /weather, GET /api/cities
├── model/
│   ├── GeoLocation.java          # @Data, @JsonIgnoreProperties
│   ├── GeocodingResponse.java
│   ├── WeatherData.java
│   └── WeatherViewModel.java     # @Data — passed to Thymeleaf
└── service/
    ├── GeocodingService.java     # @Cacheable(GEOCODING_CACHE), key = query.toLowerCase()+'-'+count
    ├── WeatherCodeService.java   # WMO code → description / icon / CSS class
    └── WeatherService.java       # @Cacheable(WEATHER_CACHE), key = lat+','+lon
```

## Key Architectural Decisions

### HTTP Client
Single `HttpClient` bean named `"unTrustedHttpClient"` (TLS verification disabled — Open-Meteo uses valid certs but the name is kept for clarity). Both `WeatherService` and `GeocodingService` inject it via `@Qualifier("unTrustedHttpClient")`.

### Cache
- `CacheConfig.WEATHER_CACHE = "weather"` — 10 minutes TTL, max 100 entries
- `CacheConfig.GEOCODING_CACHE = "geocoding"` — 24 hours TTL, max 500 entries
- `GeocodingService.searchCities` has condition `#query.length() >= 2` — queries shorter than 2 chars bypass cache

### Jackson
Uses Jackson 3 (`tools.jackson.*`) — **not** the classic `com.fasterxml.jackson.*`. Imports must use `tools.jackson.databind.ObjectMapper`, `tools.jackson.databind.json.JsonMapper`, etc.

### Models
All models use Lombok `@Data`. Do **not** write manual getters/setters.

## Coding Conventions

- **No MapStruct** — use manual mapping
- **No field injection** — constructor injection only (`@RequiredArgsConstructor` or explicit constructor)
- **No manual logger** — use `private static final Logger log = LoggerFactory.getLogger(...)` (Lombok `@Log4j2` not used here)
- **Lombok** requires explicit `annotationProcessorPaths` in `maven-compiler-plugin` (already configured in pom.xml — do not remove)
- Response format: `Model` for Thymeleaf views, `@ResponseBody` for JSON endpoints

## Testing

### Setup Notes (Spring Boot 4.x specifics)

`spring-boot-starter-security` and `spring-security-test` are **test-scope only** dependencies. They are required because `spring-boot-starter-actuator` in Spring Boot 4.x transitively references `org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration`. Without security on the test classpath, the application context fails to start in tests with `FileNotFoundException`.

### Controller Tests (`WeatherControllerTest`)
- Use `@SpringBootTest` + `@AutoConfigureMockMvc(addFilters = false)`
- `addFilters = false` disables all security filters in MockMvc (correct for fully public endpoints)
- Do **not** use `@WebMvcTest` — it triggers security auto-configuration issues in Spring Boot 4.x

### Service Tests (`WeatherServiceTest`, `GeocodingServiceTest`)
- Use `@ExtendWith(MockitoExtension.class)` — no Spring context needed
- Mock `HttpClient` with Mockito; use `doReturn(...).when(httpClient).send(...)` pattern
- Inject `forecastBaseUrl` / `geocodingBaseUrl` via `ReflectionTestUtils.setField`

### Cache Integration Tests (`CacheIntegrationTest`)
- Use `@SpringBootTest` + `@MockitoBean` for `HttpClient`
- `@MockitoBean` replaces the `"unTrustedHttpClient"` bean in the context (single `HttpClient` bean → resolved by type)
- Clear all caches in `@BeforeEach` via `CacheManager`

### Mock pattern
Prefer `doReturn(...).when(mock).method(...)` over `when(mock.method(...)).thenReturn(...)` (per project convention).

## pom.xml Highlights

```xml
<!-- Spring Boot 4.x parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
</parent>

<!-- Lombok requires explicit annotation processor path in SB4 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>

<!-- Security: test scope only — do NOT add to main scope -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Known Spring Boot 4.x Gotchas

1. **Lombok**: must be in `annotationProcessorPaths` — not auto-detected like in SB3
2. **Security in tests**: actuator pulls in `SecurityAutoConfiguration` reference → add `spring-boot-starter-security` as test-scope to put the class on the classpath
3. **Jackson 3**: package is `tools.jackson.*`, not `com.fasterxml.jackson.*`
4. **`@WebMvcTest`**: avoid for this project — security slice causes cascading auto-configuration failures; use `@SpringBootTest + @AutoConfigureMockMvc(addFilters = false)` instead
5. **`@MockitoBean`**: from `org.springframework.test.context.bean.override.mockito.MockitoBean` (Spring Framework 6.2+, moved from Spring Boot Test)
