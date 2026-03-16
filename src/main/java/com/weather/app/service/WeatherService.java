package com.weather.app.service;

import com.weather.app.model.GeoLocation;
import com.weather.app.model.WeatherData;
import com.weather.app.model.WeatherViewModel;
import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private static final String CURRENT_PARAMS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation," +
            "weather_code,wind_speed_10m,wind_direction_10m,surface_pressure," +
            "cloud_cover,visibility,uv_index,is_day";

    private static final String HOURLY_PARAMS =
            "temperature_2m,precipitation_probability,weather_code,wind_speed_10m";

    private static final String DAILY_PARAMS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum," +
            "precipitation_probability_max,wind_speed_10m_max,sunrise,sunset,uv_index_max";

    @Value("${weather.forecast.url}")
    private String forecastBaseUrl;

    private final HttpClient unTrustedHttpClient;
    private final ObjectMapper objectMapper;
    private final WeatherCodeService weatherCodeService;

    public WeatherService(@Qualifier("unTrustedHttpClient") HttpClient unTrustedHttpClient,
                          ObjectMapper objectMapper, WeatherCodeService weatherCodeService) {
        this.unTrustedHttpClient = unTrustedHttpClient;
        this.objectMapper = objectMapper;
        this.weatherCodeService = weatherCodeService;
    }

    public WeatherViewModel getWeather(GeoLocation location) throws IOException, InterruptedException {
        String url = forecastBaseUrl + "/forecast"
                + "?latitude=" + location.getLatitude()
                + "&longitude=" + location.getLongitude()
                + "&current=" + CURRENT_PARAMS
                + "&hourly=" + HOURLY_PARAMS
                + "&daily=" + DAILY_PARAMS
                + "&timezone=auto"
                + "&forecast_days=7"
                + "&wind_speed_unit=kmh";

        log.debug("Weather request: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = unTrustedHttpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Weather API returned status: " + response.statusCode());
        }

        WeatherData data = objectMapper.readValue(response.body(), WeatherData.class);
        return buildViewModel(location, data);
    }

    private WeatherViewModel buildViewModel(GeoLocation location, WeatherData data) {
        WeatherViewModel vm = new WeatherViewModel();
        vm.setCityName(location.getName());
        vm.setCityDisplayName(location.getDisplayName());
        vm.setLatitude(location.getLatitude());
        vm.setLongitude(location.getLongitude());
        vm.setTimezone(data.getTimezone());
        vm.setElevation(data.getElevation());

        WeatherData.CurrentWeather curr = data.getCurrent();
        if (curr != null) {
            boolean isDay = curr.getIsDay() != null && curr.getIsDay() == 1;
            int code = curr.getWeatherCode() != null ? curr.getWeatherCode() : 0;

            WeatherViewModel.CurrentInfo info = new WeatherViewModel.CurrentInfo();
            info.setTime(formatDateTime(curr.getTime()));
            info.setTemperature(round1(curr.getTemperature()));
            info.setFeelsLike(round1(curr.getFeelsLike()));
            info.setHumidity(curr.getHumidity());
            info.setPrecipitation(curr.getPrecipitation());
            info.setWindSpeed(round1(curr.getWindSpeed()));
            info.setWindDirection(curr.getWindDirection());
            info.setWindDirectionLabel(curr.getWindDirection() != null
                    ? weatherCodeService.getWindDirection(curr.getWindDirection()) : "-");
            info.setPressure(curr.getPressure() != null ? Math.round(curr.getPressure() * 10.0) / 10.0 : null);
            info.setCloudCover(curr.getCloudCover());
            info.setVisibilityKm(curr.getVisibility() != null ? round1(curr.getVisibility() / 1000.0) : null);
            info.setUvIndex(curr.getUvIndex());
            info.setDay(isDay);
            info.setWeatherCode(code);
            info.setWeatherDescription(weatherCodeService.getDescription(code));
            info.setWeatherIcon(weatherCodeService.getIcon(code, isDay));
            info.setBackgroundClass(weatherCodeService.getBackgroundClass(code, isDay));
            vm.setCurrent(info);
        }

        WeatherData.DailyWeather daily = data.getDaily();
        if (daily != null && daily.getTime() != null) {
            List<WeatherViewModel.DailyForecast> forecasts = new ArrayList<>();
            String today = LocalDate.now().toString();
            for (int i = 0; i < daily.getTime().size(); i++) {
                WeatherViewModel.DailyForecast f = new WeatherViewModel.DailyForecast();
                String date = daily.getTime().get(i);
                f.setIsoDate(date);
                f.setDate(formatDate(date));
                f.setDayName(getDayName(date));
                f.setToday(date.equals(today));

                int code = safeGet(daily.getWeatherCode(), i, 0);
                f.setWeatherCode(code);
                f.setWeatherDescription(weatherCodeService.getDescription(code));
                f.setWeatherIcon(weatherCodeService.getIcon(code, true));
                f.setTempMax(round1(safeGetDouble(daily.getTemperatureMax(), i)));
                f.setTempMin(round1(safeGetDouble(daily.getTemperatureMin(), i)));
                f.setPrecipitationSum(round1(safeGetDouble(daily.getPrecipitationSum(), i)));
                f.setPrecipitationProbability(safeGet(daily.getPrecipitationProbabilityMax(), i, null));
                f.setWindSpeedMax(round1(safeGetDouble(daily.getWindSpeedMax(), i)));
                f.setSunrise(formatTime(safeGetStr(daily.getSunrise(), i)));
                f.setSunset(formatTime(safeGetStr(daily.getSunset(), i)));
                f.setUvIndexMax(round1(safeGetDouble(daily.getUvIndexMax(), i)));
                forecasts.add(f);
            }
            vm.setDailyForecasts(forecasts);
        }

        WeatherData.HourlyWeather hourly = data.getHourly();
        if (hourly != null && hourly.getTime() != null) {
            List<WeatherViewModel.HourlySlot> slots = new ArrayList<>();
            List<WeatherViewModel.HourlySlot> allSlots = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            int count = 0;
            for (int i = 0; i < hourly.getTime().size(); i++) {
                String timeStr = hourly.getTime().get(i);
                LocalDateTime slotTime = LocalDateTime.parse(timeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

                WeatherViewModel.HourlySlot slot = new WeatherViewModel.HourlySlot();
                slot.setTime(timeStr);
                slot.setDisplayTime(slotTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                slot.setTemperature(round1(safeGetDouble(hourly.getTemperature(), i)));
                slot.setPrecipitationProbability(safeGet(hourly.getPrecipitationProbability(), i, null));
                int code = safeGet(hourly.getWeatherCode(), i, 0);
                slot.setWeatherCode(code);
                slot.setWeatherIcon(weatherCodeService.getIcon(code, slotTime.getHour() >= 7 && slotTime.getHour() < 20));
                slot.setWindSpeed(round1(safeGetDouble(hourly.getWindSpeed(), i)));
                allSlots.add(slot);

                if (!slotTime.isBefore(now) && count < 24) {
                    slots.add(slot);
                    count++;
                }
            }
            vm.setNextHours(slots);
            vm.setAllHourlySlots(allSlots);
        }

        return vm;
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        try {
            LocalDateTime dt = LocalDateTime.parse(iso, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) { return iso; }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null) return "-";
        try {
            LocalDate d = LocalDate.parse(isoDate);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) { return isoDate; }
    }

    private String formatTime(String isoDateTime) {
        if (isoDateTime == null) return "-";
        try {
            LocalDateTime dt = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            if (isoDateTime.contains("T")) {
                return isoDateTime.substring(isoDateTime.indexOf('T') + 1, Math.min(isoDateTime.indexOf('T') + 6, isoDateTime.length()));
            }
            return isoDateTime;
        }
    }

    private String getDayName(String isoDate) {
        if (isoDate == null) return "-";
        try {
            LocalDate d = LocalDate.parse(isoDate);
            if (d.equals(LocalDate.now())) return "Oggi";
            if (d.equals(LocalDate.now().plusDays(1))) return "Domani";
            return d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ITALIAN);
        } catch (Exception e) { return isoDate; }
    }

    private Double round1(Double val) {
        if (val == null) return null;
        return Math.round(val * 10.0) / 10.0;
    }

    private <T> T safeGet(List<T> list, int index, T defaultVal) {
        if (list == null || index >= list.size()) return defaultVal;
        T val = list.get(index);
        return val != null ? val : defaultVal;
    }

    private Double safeGetDouble(List<Double> list, int index) {
        return safeGet(list, index, null);
    }

    private String safeGetStr(List<String> list, int index) {
        return safeGet(list, index, null);
    }
}
