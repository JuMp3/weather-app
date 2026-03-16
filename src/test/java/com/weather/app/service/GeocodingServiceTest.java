package com.weather.app.service;

import com.weather.app.model.GeoLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeocodingService")
class GeocodingServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private GeocodingService service;

    // Single valid API response — reused across tests
    private static final String GEOCODING_JSON =
            "{\"results\":[{\"id\":1,\"name\":\"Roma\",\"latitude\":41.89," +
            "\"longitude\":12.48,\"country\":\"Italy\",\"country_code\":\"IT\"," +
            "\"admin1\":\"Lazio\",\"timezone\":\"Europe/Rome\"}]}";

    @BeforeEach
    void setUp() {
        service = new GeocodingService(httpClient, JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "geocodingBaseUrl", "https://geocoding-api.test");
    }

    @AfterEach
    void clearInterruptFlag() {
        // Make sure each test starts with a clean interrupt status.
        Thread.interrupted();
    }

    // -------------------------------------------------------------------------
    // searchCities
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("searchCities")
    class SearchCitiesTest {

        @Test
        @DisplayName("success → returns list of GeoLocation")
        void success() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(GEOCODING_JSON);
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            List<GeoLocation> result = service.searchCities("Roma", 5);

            assertThat(result).hasSize(1);
            GeoLocation loc = result.getFirst();
            assertThat(loc.getName()).isEqualTo("Roma");
            assertThat(loc.getLatitude()).isEqualTo(41.89);
            assertThat(loc.getLongitude()).isEqualTo(12.48);
            assertThat(loc.getCountry()).isEqualTo("Italy");
            assertThat(loc.getCountryCode()).isEqualTo("IT");
            assertThat(loc.getRegion()).isEqualTo("Lazio");
        }

        @Test
        @DisplayName("null query → returns empty list, no HTTP call made")
        void nullQuery() throws Exception {
            List<GeoLocation> result = service.searchCities(null, 5);

            assertThat(result).isEmpty();
            verify(httpClient, never()).send(any(), any());
        }

        @Test
        @DisplayName("query shorter than 2 chars → returns empty list, no HTTP call made")
        void queryTooShort() throws Exception {
            List<GeoLocation> result = service.searchCities("R", 5);

            assertThat(result).isEmpty();
            verify(httpClient, never()).send(any(), any());
        }

        @Test
        @DisplayName("blank query (only spaces) → returns empty list, no HTTP call made")
        void blankQuery() throws Exception {
            List<GeoLocation> result = service.searchCities("  ", 5);

            assertThat(result).isEmpty();
            verify(httpClient, never()).send(any(), any());
        }

        @Test
        @DisplayName("API returns non-200 status → returns empty list")
        void nonOkStatus() throws Exception {
            when(mockResponse.statusCode()).thenReturn(500);
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            List<GeoLocation> result = service.searchCities("Roma", 5);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("IOException during send → returns empty list")
        void ioException() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any()))
                    .thenThrow(new IOException("network error"));

            List<GeoLocation> result = service.searchCities("Roma", 5);

            assertThat(result).isEmpty();
            // Thread interrupt flag must NOT be set for IOException
            assertThat(Thread.currentThread().isInterrupted()).isFalse();
        }

        @Test
        @DisplayName("InterruptedException during send → returns empty list AND thread is interrupted")
        void interruptedException() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any()))
                    .thenThrow(new InterruptedException("interrupted"));

            List<GeoLocation> result = service.searchCities("Roma", 5);

            assertThat(result).isEmpty();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            // Clear flag for subsequent tests (also handled in @AfterEach)
            Thread.interrupted();
        }

        @Test
        @DisplayName("API returns JSON with null results field → returns empty list")
        void nullResultsField() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"results\":null}");
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            List<GeoLocation> result = service.searchCities("Roma", 5);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("API returns JSON with missing results field → returns empty list")
        void missingResultsField() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"generationtime_ms\":1.5}");
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            List<GeoLocation> result = service.searchCities("Roma", 5);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // findCity
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findCity")
    class FindCityTest {

        private void stubHttpSuccess() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(GEOCODING_JSON);
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());
        }

        @Test
        @DisplayName("lat/lon matching the single result → returns that location")
        void latLonMatching() throws Exception {
            stubHttpSuccess();

            GeoLocation result = service.findCity("Roma", 41.89, 12.48);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Roma");
        }

        @Test
        @DisplayName("lat/lon not matching any result → returns first result as fallback")
        void latLonNotMatching() throws Exception {
            stubHttpSuccess();

            GeoLocation result = service.findCity("Roma", 10.0, 10.0);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Roma");
        }

        @Test
        @DisplayName("lat/lon null → returns first result")
        void latLonNull() throws Exception {
            stubHttpSuccess();

            GeoLocation result = service.findCity("Roma", null, null);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Roma");
        }

        @Test
        @DisplayName("no results from API → returns null")
        void noResults() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"results\":null}");
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            GeoLocation result = service.findCity("Unknown", null, null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("lat matches but lon does not → falls back to first result")
        void latMatchLonMismatch() throws Exception {
            stubHttpSuccess();

            // lat matches (diff < 0.01) but lon does not
            GeoLocation result = service.findCity("Roma", 41.895, 99.0);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Roma");
        }

        @Test
        @DisplayName("lat/lon within proximity threshold → returns matching location")
        void withinProximityThreshold() throws Exception {
            stubHttpSuccess();

            // Both within 0.01 tolerance
            GeoLocation result = service.findCity("Roma", 41.8905, 12.4805);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Roma");
        }
    }
}
