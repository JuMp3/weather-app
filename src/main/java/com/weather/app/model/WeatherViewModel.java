package com.weather.app.model;

import lombok.Data;

import java.util.List;

@Data
public class WeatherViewModel {

    private String cityName;
    private String cityDisplayName;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private Double elevation;

    private CurrentInfo current;
    private List<DailyForecast> dailyForecasts;
    private List<HourlySlot> nextHours;
    private List<HourlySlot> allHourlySlots;

    @Data
    public static class CurrentInfo {
        private String time;
        private Double temperature;
        private Double feelsLike;
        private Integer humidity;
        private Double precipitation;
        private Double windSpeed;
        private Integer windDirection;
        private String windDirectionLabel;
        private Double pressure;
        private Integer cloudCover;
        private Double visibilityKm;
        private Double uvIndex;
        private boolean isDay;
        private int weatherCode;
        private String weatherDescription;
        private String weatherIcon;
        private String backgroundClass;
    }

    @Data
    public static class DailyForecast {
        private String isoDate;
        private String date;
        private String dayName;
        private int weatherCode;
        private String weatherDescription;
        private String weatherIcon;
        private Double tempMax;
        private Double tempMin;
        private Double precipitationSum;
        private Integer precipitationProbability;
        private Double windSpeedMax;
        private String sunrise;
        private String sunset;
        private Double uvIndexMax;
        private boolean isToday;
    }

    @Data
    public static class HourlySlot {
        private String time;
        private String displayTime;
        private Double temperature;
        private Integer precipitationProbability;
        private int weatherCode;
        private String weatherIcon;
        private Double windSpeed;
    }
}
