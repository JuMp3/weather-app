package com.weather.app.service;

import com.weather.app.model.GeoLocation;
import com.weather.app.model.WeatherViewModel;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService")
class WeatherServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private WeatherService service;

    private GeoLocation location;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @BeforeEach
    void setUp() {
        WeatherCodeService weatherCodeService = new WeatherCodeService();
        service = new WeatherService(httpClient, JsonMapper.builder().build(), weatherCodeService);
        ReflectionTestUtils.setField(service, "forecastBaseUrl", "https://api.weather.test");

        location = new GeoLocation();
        location.setName("Roma");
        location.setRegion("Lazio");
        location.setCountry("Italy");
        location.setLatitude(41.89);
        location.setLongitude(12.48);
        location.setTimezone("Europe/Rome");
        location.setElevation(21.0);
    }

    // -------------------------------------------------------------------------
    // JSON builder helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a complete weather JSON using today's date so that the
     * "isToday" flag is correctly evaluated inside buildViewModel.
     * Hourly slots: 2 past slots (now-2h, now-1h) + 3 future slots (now+1h..now+3h).
     */
    private String buildFullWeatherJson() {
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime h_m2 = now.minusHours(2);
        LocalDateTime h_m1 = now.minusHours(1);
        LocalDateTime h_p1 = now.plusHours(1);
        LocalDateTime h_p2 = now.plusHours(2);
        LocalDateTime h_p3 = now.plusHours(3);

        String todayStr    = today.format(DATE_FMT);
        String tomorrowStr = tomorrow.format(DATE_FMT);
        String currentTime = now.format(DT_FMT);

        return "{"
            + "\"latitude\":41.89,"
            + "\"longitude\":12.48,"
            + "\"timezone\":\"Europe/Rome\","
            + "\"elevation\":21.0,"
            + "\"current\":{"
            +   "\"time\":\"" + currentTime + "\","
            +   "\"temperature_2m\":20.5,"
            +   "\"relative_humidity_2m\":65,"
            +   "\"apparent_temperature\":19.0,"
            +   "\"precipitation\":0.0,"
            +   "\"weather_code\":0,"
            +   "\"wind_speed_10m\":10.5,"
            +   "\"wind_direction_10m\":180,"
            +   "\"surface_pressure\":1013.5,"
            +   "\"cloud_cover\":10,"
            +   "\"visibility\":24000.0,"
            +   "\"uv_index\":5.0,"
            +   "\"is_day\":1"
            + "},"
            + "\"hourly\":{"
            +   "\"time\":["
            +     "\"" + h_m2.format(DT_FMT) + "\","
            +     "\"" + h_m1.format(DT_FMT) + "\","
            +     "\"" + h_p1.format(DT_FMT) + "\","
            +     "\"" + h_p2.format(DT_FMT) + "\","
            +     "\"" + h_p3.format(DT_FMT) + "\""
            +   "],"
            +   "\"temperature_2m\":[18.0,19.0,21.0,22.0,21.5],"
            +   "\"precipitation_probability\":[0,0,10,20,30],"
            +   "\"weather_code\":[0,0,0,1,2],"
            +   "\"wind_speed_10m\":[8.0,9.0,11.0,12.0,10.0]"
            + "},"
            + "\"daily\":{"
            +   "\"time\":[\"" + todayStr + "\",\"" + tomorrowStr + "\"],"
            +   "\"weather_code\":[0,1],"
            +   "\"temperature_2m_max\":[24.0,22.0],"
            +   "\"temperature_2m_min\":[14.0,13.0],"
            +   "\"precipitation_sum\":[0.0,1.5],"
            +   "\"precipitation_probability_max\":[5,30],"
            +   "\"wind_speed_10m_max\":[15.0,20.0],"
            +   "\"sunrise\":[\"" + todayStr + "T06:00\",\"" + tomorrowStr + "T06:01\"],"
            +   "\"sunset\":[\"" + todayStr + "T20:00\",\"" + tomorrowStr + "T19:59\"],"
            +   "\"uv_index_max\":[5.0,4.5]"
            + "}"
            + "}";
    }

    /** Weather JSON where current is explicitly null. */
    private String buildJsonCurrentNull() {
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        String todayStr    = today.format(DATE_FMT);
        String tomorrowStr = tomorrow.format(DATE_FMT);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime h_p1 = now.plusHours(1);

        return "{"
            + "\"latitude\":41.89,"
            + "\"longitude\":12.48,"
            + "\"timezone\":\"Europe/Rome\","
            + "\"elevation\":21.0,"
            + "\"current\":null,"
            + "\"hourly\":{"
            +   "\"time\":[\"" + h_p1.format(DT_FMT) + "\"],"
            +   "\"temperature_2m\":[20.0],"
            +   "\"precipitation_probability\":[0],"
            +   "\"weather_code\":[0],"
            +   "\"wind_speed_10m\":[5.0]"
            + "},"
            + "\"daily\":{"
            +   "\"time\":[\"" + todayStr + "\",\"" + tomorrowStr + "\"],"
            +   "\"weather_code\":[0,1],"
            +   "\"temperature_2m_max\":[24.0,22.0],"
            +   "\"temperature_2m_min\":[14.0,13.0],"
            +   "\"precipitation_sum\":[0.0,1.5],"
            +   "\"precipitation_probability_max\":[5,30],"
            +   "\"wind_speed_10m_max\":[15.0,20.0],"
            +   "\"sunrise\":[\"" + todayStr + "T06:00\",\"" + tomorrowStr + "T06:01\"],"
            +   "\"sunset\":[\"" + todayStr + "T20:00\",\"" + tomorrowStr + "T19:59\"],"
            +   "\"uv_index_max\":[5.0,4.5]"
            + "}"
            + "}";
    }

    /** Weather JSON where daily and hourly are both null. */
    private String buildJsonDailyHourlyNull() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        return "{"
            + "\"latitude\":41.89,"
            + "\"longitude\":12.48,"
            + "\"timezone\":\"Europe/Rome\","
            + "\"elevation\":21.0,"
            + "\"current\":{"
            +   "\"time\":\"" + now.format(DT_FMT) + "\","
            +   "\"temperature_2m\":20.5,"
            +   "\"relative_humidity_2m\":65,"
            +   "\"apparent_temperature\":19.0,"
            +   "\"precipitation\":0.0,"
            +   "\"weather_code\":0,"
            +   "\"wind_speed_10m\":10.5,"
            +   "\"wind_direction_10m\":180,"
            +   "\"surface_pressure\":1013.5,"
            +   "\"cloud_cover\":10,"
            +   "\"visibility\":24000.0,"
            +   "\"uv_index\":5.0,"
            +   "\"is_day\":1"
            + "},"
            + "\"daily\":null,"
            + "\"hourly\":null"
            + "}";
    }

    // -------------------------------------------------------------------------
    // getWeather
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getWeather")
    class GetWeatherTest {

        @Test
        @DisplayName("success → ViewModel populated with all fields")
        void success() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(buildFullWeatherJson());
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            WeatherViewModel vm = service.getWeather(location);

            // City / location fields
            assertThat(vm.getCityName()).isEqualTo("Roma");
            assertThat(vm.getCityDisplayName()).isEqualTo("Roma, Lazio, Italy");
            assertThat(vm.getTimezone()).isEqualTo("Europe/Rome");
            assertThat(vm.getElevation()).isEqualTo(21.0);
            assertThat(vm.getLatitude()).isEqualTo(41.89);
            assertThat(vm.getLongitude()).isEqualTo(12.48);

            // Current weather
            WeatherViewModel.CurrentInfo curr = vm.getCurrent();
            assertThat(curr).isNotNull();
            assertThat(curr.getTemperature()).isEqualTo(20.5);
            assertThat(curr.getHumidity()).isEqualTo(65);
            assertThat(curr.getFeelsLike()).isEqualTo(19.0);
            assertThat(curr.getPrecipitation()).isEqualTo(0.0);
            assertThat(curr.getWeatherCode()).isEqualTo(0);
            assertThat(curr.getWindSpeed()).isEqualTo(10.5);
            assertThat(curr.getWindDirection()).isEqualTo(180);
            assertThat(curr.getWindDirectionLabel()).isEqualTo("S");
            assertThat(curr.getPressure()).isEqualTo(1013.5);
            assertThat(curr.getCloudCover()).isEqualTo(10);
            // visibility 24000.0 m → 24.0 km
            assertThat(curr.getVisibilityKm()).isEqualTo(24.0);
            assertThat(curr.getUvIndex()).isEqualTo(5.0);
            assertThat(curr.isDay()).isTrue();
            assertThat(curr.getWeatherDescription()).isEqualTo("Cielo sereno");
            assertThat(curr.getWeatherIcon()).isEqualTo("☀️");
            assertThat(curr.getBackgroundClass()).isEqualTo("bg-sunny");

            // Daily forecasts
            assertThat(vm.getDailyForecasts()).hasSize(2);
            WeatherViewModel.DailyForecast todayForecast = vm.getDailyForecasts().getFirst();
            assertThat(todayForecast.isToday()).isTrue();
            assertThat(todayForecast.getIsoDate()).isEqualTo(LocalDate.now().format(DATE_FMT));
            assertThat(todayForecast.getTempMax()).isEqualTo(24.0);
            assertThat(todayForecast.getTempMin()).isEqualTo(14.0);

            WeatherViewModel.DailyForecast tomorrowForecast = vm.getDailyForecasts().get(1);
            assertThat(tomorrowForecast.isToday()).isFalse();

            // nextHours: only future slots (3 in our JSON) — at most 24
            assertThat(vm.getNextHours()).isNotNull();
            assertThat(vm.getNextHours().size()).isGreaterThan(0);
            assertThat(vm.getNextHours().size()).isLessThanOrEqualTo(24);

            // allHourlySlots: all 5 slots (past + future)
            assertThat(vm.getAllHourlySlots()).hasSize(5);
        }

        @Test
        @DisplayName("HTTP non-200 → throws IOException")
        void nonOkStatusThrows() throws Exception {
            when(mockResponse.statusCode()).thenReturn(503);
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            assertThatThrownBy(() -> service.getWeather(location))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("503");
        }

        @Test
        @DisplayName("current is null in response → CurrentInfo is null in ViewModel")
        void currentNull() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(buildJsonCurrentNull());
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            WeatherViewModel vm = service.getWeather(location);

            assertThat(vm.getCurrent()).isNull();
            // But daily forecasts should still be populated
            assertThat(vm.getDailyForecasts()).hasSize(2);
        }

        @Test
        @DisplayName("daily and hourly are null → forecasts empty/null and nextHours empty/null")
        void dailyAndHourlyNull() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(buildJsonDailyHourlyNull());
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            WeatherViewModel vm = service.getWeather(location);

            // Current should still be populated
            assertThat(vm.getCurrent()).isNotNull();

            // Daily and hourly sections must be null or empty
            assertThat(vm.getDailyForecasts()).isNullOrEmpty();
            assertThat(vm.getNextHours()).isNullOrEmpty();
            assertThat(vm.getAllHourlySlots()).isNullOrEmpty();
        }

        @Test
        @DisplayName("nextHours contains only future slots (not past ones)")
        void nextHoursContainOnlyFutureSlots() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(buildFullWeatherJson());
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            WeatherViewModel vm = service.getWeather(location);

            LocalDateTime now = LocalDateTime.now();
            if (vm.getNextHours() != null) {
                for (WeatherViewModel.HourlySlot slot : vm.getNextHours()) {
                    LocalDateTime slotTime = LocalDateTime.parse(slot.getTime(), DT_FMT);
                    assertThat(slotTime).isAfterOrEqualTo(now.truncatedTo(ChronoUnit.HOURS));
                }
            }
        }

        @Test
        @DisplayName("allHourlySlots contains all slots (past and future)")
        void allHourlyContainsAll() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(buildFullWeatherJson());
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            WeatherViewModel vm = service.getWeather(location);

            // 2 past + 3 future = 5 total
            assertThat(vm.getAllHourlySlots()).hasSize(5);
        }

        @Test
        @DisplayName("today's daily entry has isToday=true, tomorrow's has isToday=false")
        void isTodayFlagSetCorrectly() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(buildFullWeatherJson());
            doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());

            WeatherViewModel vm = service.getWeather(location);

            assertThat(vm.getDailyForecasts()).hasSize(2);
            assertThat(vm.getDailyForecasts().get(0).isToday()).isTrue();
            assertThat(vm.getDailyForecasts().get(1).isToday()).isFalse();
        }
    }
}
