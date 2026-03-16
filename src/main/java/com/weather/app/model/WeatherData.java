package com.weather.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherData {

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("elevation")
    private Double elevation;

    @JsonProperty("current")
    private CurrentWeather current;

    @JsonProperty("hourly")
    private HourlyWeather hourly;

    @JsonProperty("daily")
    private DailyWeather daily;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrentWeather {
        @JsonProperty("time")
        private String time;

        @JsonProperty("temperature_2m")
        private Double temperature;

        @JsonProperty("relative_humidity_2m")
        private Integer humidity;

        @JsonProperty("apparent_temperature")
        private Double feelsLike;

        @JsonProperty("precipitation")
        private Double precipitation;

        @JsonProperty("weather_code")
        private Integer weatherCode;

        @JsonProperty("wind_speed_10m")
        private Double windSpeed;

        @JsonProperty("wind_direction_10m")
        private Integer windDirection;

        @JsonProperty("surface_pressure")
        private Double pressure;

        @JsonProperty("cloud_cover")
        private Integer cloudCover;

        @JsonProperty("visibility")
        private Double visibility;

        @JsonProperty("uv_index")
        private Double uvIndex;

        @JsonProperty("is_day")
        private Integer isDay;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HourlyWeather {
        @JsonProperty("time")
        private List<String> time;

        @JsonProperty("temperature_2m")
        private List<Double> temperature;

        @JsonProperty("precipitation_probability")
        private List<Integer> precipitationProbability;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;

        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeed;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyWeather {
        @JsonProperty("time")
        private List<String> time;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;

        @JsonProperty("temperature_2m_max")
        private List<Double> temperatureMax;

        @JsonProperty("temperature_2m_min")
        private List<Double> temperatureMin;

        @JsonProperty("precipitation_sum")
        private List<Double> precipitationSum;

        @JsonProperty("precipitation_probability_max")
        private List<Integer> precipitationProbabilityMax;

        @JsonProperty("wind_speed_10m_max")
        private List<Double> windSpeedMax;

        @JsonProperty("sunrise")
        private List<String> sunrise;

        @JsonProperty("sunset")
        private List<String> sunset;

        @JsonProperty("uv_index_max")
        private List<Double> uvIndexMax;
    }
}
