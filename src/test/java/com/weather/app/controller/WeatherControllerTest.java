package com.weather.app.controller;

import com.weather.app.model.GeoLocation;
import com.weather.app.model.WeatherViewModel;
import com.weather.app.service.GeocodingService;
import com.weather.app.service.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "weather.forecast.url=https://api.weather.test",
        "weather.geocoding.url=https://geocoding.test"
})
@DisplayName("WeatherController")
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @MockitoBean
    private GeocodingService geocodingService;

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    private GeoLocation buildTestGeoLocation() {
        GeoLocation loc = new GeoLocation();
        loc.setId(1L);
        loc.setName("Roma");
        loc.setRegion("Lazio");
        loc.setCountry("Italy");
        loc.setCountryCode("IT");
        loc.setLatitude(41.89);
        loc.setLongitude(12.48);
        loc.setTimezone("Europe/Rome");
        loc.setElevation(21.0);
        return loc;
    }

    private WeatherViewModel buildTestViewModel() {
        WeatherViewModel vm = new WeatherViewModel();
        vm.setCityName("Roma");
        vm.setCityDisplayName("Roma, Lazio, Italy");
        vm.setLatitude(41.89);
        vm.setLongitude(12.48);
        vm.setTimezone("Europe/Rome");
        vm.setElevation(21.0);

        WeatherViewModel.CurrentInfo current = new WeatherViewModel.CurrentInfo();
        current.setTime("16/03/2026 12:00");
        current.setTemperature(20.5);
        current.setFeelsLike(19.0);
        current.setHumidity(65);
        current.setPrecipitation(0.0);
        current.setWeatherCode(0);
        current.setWindSpeed(10.5);
        current.setWindDirection(180);
        current.setWindDirectionLabel("S");
        current.setPressure(1013.5);
        current.setCloudCover(10);
        current.setVisibilityKm(24.0);
        current.setUvIndex(5.0);
        current.setDay(true);
        current.setWeatherDescription("Cielo sereno");
        current.setWeatherIcon("☀️");
        current.setBackgroundClass("bg-sunny");
        vm.setCurrent(current);

        vm.setDailyForecasts(new ArrayList<>());
        vm.setNextHours(new ArrayList<>());
        vm.setAllHourlySlots(new ArrayList<>());

        return vm;
    }

    // -------------------------------------------------------------------------
    // GET /
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /")
    class IndexTest {

        @Test
        @DisplayName("returns status 200 and view 'index'")
        void indexPage() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"));
        }

        @Test
        @DisplayName("model contains 'title' attribute")
        void indexHasTitle() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("title"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /weather
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /weather")
    class WeatherTest {

        @Test
        @DisplayName("valid city with lat/lon → status 200, view 'weather', model has 'weather'")
        void successWithLatLon() throws Exception {
            GeoLocation loc = buildTestGeoLocation();
            WeatherViewModel vm = buildTestViewModel();

            when(geocodingService.findCity(eq("Roma"), anyDouble(), anyDouble())).thenReturn(loc);
            when(weatherService.getWeather(loc)).thenReturn(vm);

            mockMvc.perform(get("/weather")
                            .param("city", "Roma")
                            .param("lat", "41.89")
                            .param("lon", "12.48"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("weather"))
                    .andExpect(model().attributeExists("weather"));
        }

        @Test
        @DisplayName("blank city param → view 'index', model has 'error'")
        void blankCity() throws Exception {
            mockMvc.perform(get("/weather").param("city", "   "))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("empty city param → view 'index', model has 'error'")
        void emptyCity() throws Exception {
            mockMvc.perform(get("/weather").param("city", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("geocodingService returns null → view 'index', model has 'error'")
        void cityNotFound() throws Exception {
            when(geocodingService.findCity(anyString(), isNull(), isNull())).thenReturn(null);

            mockMvc.perform(get("/weather").param("city", "Unknown"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error",
                            containsString("Unknown")));
        }

        @Test
        @DisplayName("weatherService throws IOException → view 'index', model has 'error'")
        void weatherServiceThrowsIOException() throws Exception {
            GeoLocation loc = buildTestGeoLocation();
            when(geocodingService.findCity(anyString(), isNull(), isNull())).thenReturn(loc);
            when(weatherService.getWeather(any(GeoLocation.class)))
                    .thenThrow(new IOException("network error"));

            mockMvc.perform(get("/weather").param("city", "Roma"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("weatherService throws InterruptedException → view 'index', model has 'error'")
        void weatherServiceThrowsInterruptedException() throws Exception {
            GeoLocation loc = buildTestGeoLocation();
            when(geocodingService.findCity(anyString(), isNull(), isNull())).thenReturn(loc);
            when(weatherService.getWeather(any(GeoLocation.class)))
                    .thenThrow(new InterruptedException("interrupted"));

            mockMvc.perform(get("/weather").param("city", "Roma"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("success → model 'title' contains city display name")
        void titleContainsCityName() throws Exception {
            GeoLocation loc = buildTestGeoLocation();
            WeatherViewModel vm = buildTestViewModel();

            when(geocodingService.findCity(anyString(), isNull(), isNull())).thenReturn(loc);
            when(weatherService.getWeather(loc)).thenReturn(vm);

            mockMvc.perform(get("/weather").param("city", "Roma"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("weather"))
                    .andExpect(model().attribute("title",
                            containsString("Roma, Lazio, Italy")));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/cities
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/cities")
    class ApiCitiesTest {

        @Test
        @DisplayName("returns JSON array with city data")
        void returnsCityList() throws Exception {
            GeoLocation loc = buildTestGeoLocation();
            when(geocodingService.searchCities(eq("Roma"), anyInt()))
                    .thenReturn(List.of(loc));

            mockMvc.perform(get("/api/cities").param("q", "Roma"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Roma"))
                    .andExpect(jsonPath("$[0].displayName").value("Roma, Lazio, Italy"))
                    .andExpect(jsonPath("$[0].lat").value(41.89))
                    .andExpect(jsonPath("$[0].lon").value(12.48))
                    .andExpect(jsonPath("$[0].country").value("Italy"))
                    .andExpect(jsonPath("$[0].countryCode").value("IT"));
        }

        @Test
        @DisplayName("empty result from service → returns empty JSON array")
        void returnsEmptyList() throws Exception {
            when(geocodingService.searchCities(anyString(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/cities").param("q", "Xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("location with null country → country field is empty string in JSON")
        void nullCountryBecomesEmptyString() throws Exception {
            GeoLocation loc = buildTestGeoLocation();
            loc.setCountry(null);
            loc.setCountryCode(null);
            when(geocodingService.searchCities(anyString(), anyInt()))
                    .thenReturn(List.of(loc));

            mockMvc.perform(get("/api/cities").param("q", "Roma"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].country").value(""))
                    .andExpect(jsonPath("$[0].countryCode").value(""));
        }
    }
}
