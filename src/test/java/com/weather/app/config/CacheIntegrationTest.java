package com.weather.app.config;

import com.weather.app.model.GeoLocation;
import com.weather.app.model.WeatherViewModel;
import com.weather.app.service.GeocodingService;
import com.weather.app.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "weather.forecast.url=https://api.weather.test",
        "weather.geocoding.url=https://geocoding.test"
})
@DisplayName("Cache Integration")
class CacheIntegrationTest {

    @MockitoBean
    private HttpClient httpClient;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private CacheManager cacheManager;

    private static final String GEOCODING_JSON =
            "{\"results\":[{\"id\":1,\"name\":\"Roma\",\"latitude\":41.89," +
            "\"longitude\":12.48,\"country\":\"Italy\",\"country_code\":\"IT\"," +
            "\"admin1\":\"Lazio\",\"timezone\":\"Europe/Rome\"}]}";

    private static final String WEATHER_JSON =
            "{\"latitude\":41.89,\"longitude\":12.48,\"timezone\":\"Europe/Rome\"," +
            "\"elevation\":21.0,\"current\":null,\"daily\":null,\"hourly\":null}";

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @SuppressWarnings("unchecked")
    private void stubHttp(String body) throws Exception {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn(body);
        doReturn(resp).when(httpClient).send(any(HttpRequest.class), any());
    }

    private GeoLocation buildLocation(double lat, double lon) {
        GeoLocation loc = new GeoLocation();
        loc.setName("Roma");
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        return loc;
    }

    // -------------------------------------------------------------------------
    // CacheConfig structure
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CacheConfig")
    class CacheConfigTest {

        @Test
        @DisplayName("cacheManager exposes 'weather' cache")
        void hasWeatherCache() {
            assertThat(cacheManager.getCache(CacheConfig.WEATHER_CACHE)).isNotNull();
        }

        @Test
        @DisplayName("cacheManager exposes 'geocoding' cache")
        void hasGeocodingCache() {
            assertThat(cacheManager.getCache(CacheConfig.GEOCODING_CACHE)).isNotNull();
        }

        @Test
        @DisplayName("cacheManager has exactly 2 caches")
        void exactlyTwoCaches() {
            assertThat(cacheManager.getCacheNames()).hasSize(2);
        }
    }

    // -------------------------------------------------------------------------
    // WeatherService cache
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("WeatherService cache")
    class WeatherCacheTest {

        @Test
        @DisplayName("second call with same location hits cache (HTTP called once)")
        void cacheHitOnSameLocation() throws Exception {
            stubHttp(WEATHER_JSON);
            GeoLocation loc = buildLocation(41.89, 12.48);

            weatherService.getWeather(loc);
            weatherService.getWeather(loc);

            verify(httpClient, times(1)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("second call with different location misses cache (HTTP called twice)")
        void cacheMissOnDifferentLocation() throws Exception {
            stubHttp(WEATHER_JSON);

            weatherService.getWeather(buildLocation(41.89, 12.48));
            weatherService.getWeather(buildLocation(48.85, 2.35));

            verify(httpClient, times(2)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("cached result is the same object instance as the first call")
        void cachedResultIsSameInstance() throws Exception {
            stubHttp(WEATHER_JSON);
            GeoLocation loc = buildLocation(41.89, 12.48);

            WeatherViewModel first  = weatherService.getWeather(loc);
            WeatherViewModel second = weatherService.getWeather(loc);

            assertThat(second).isSameAs(first);
        }
    }

    // -------------------------------------------------------------------------
    // GeocodingService cache
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GeocodingService cache")
    class GeocodingCacheTest {

        @Test
        @DisplayName("second call with same query/count hits cache (HTTP called once)")
        void cacheHitOnSameQuery() throws Exception {
            stubHttp(GEOCODING_JSON);

            geocodingService.searchCities("Roma", 5);
            geocodingService.searchCities("Roma", 5);

            verify(httpClient, times(1)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("cache key is case-insensitive (Roma == roma)")
        void cacheKeyIsCaseInsensitive() throws Exception {
            stubHttp(GEOCODING_JSON);

            geocodingService.searchCities("Roma", 5);
            geocodingService.searchCities("roma", 5);

            verify(httpClient, times(1)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("different count parameter causes cache miss")
        void cacheMissOnDifferentCount() throws Exception {
            stubHttp(GEOCODING_JSON);

            geocodingService.searchCities("Roma", 5);
            geocodingService.searchCities("Roma", 10);

            verify(httpClient, times(2)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("different query causes cache miss")
        void cacheMissOnDifferentQuery() throws Exception {
            stubHttp(GEOCODING_JSON);

            geocodingService.searchCities("Roma", 5);
            geocodingService.searchCities("Milano", 5);

            verify(httpClient, times(2)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("query shorter than 2 chars bypasses cache and returns empty list")
        void shortQueryBypassesCache() throws Exception {
            List<GeoLocation> result = geocodingService.searchCities("R", 5);

            assertThat(result).isEmpty();
            verify(httpClient, times(0)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("null query bypasses cache and returns empty list")
        void nullQueryBypassesCache() throws Exception {
            List<GeoLocation> result = geocodingService.searchCities(null, 5);

            assertThat(result).isEmpty();
            verify(httpClient, times(0)).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("cached result is the same list instance as the first call")
        void cachedResultIsSameInstance() throws Exception {
            stubHttp(GEOCODING_JSON);

            List<GeoLocation> first  = geocodingService.searchCities("Roma", 5);
            List<GeoLocation> second = geocodingService.searchCities("Roma", 5);

            assertThat(second).isSameAs(first);
        }
    }
}
