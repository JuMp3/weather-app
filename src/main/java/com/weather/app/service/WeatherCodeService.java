package com.weather.app.service;

import org.springframework.stereotype.Service;

@Service
public class WeatherCodeService {

    public String getDescription(int code) {
        return switch (code) {
            case 0 -> "Cielo sereno";
            case 1 -> "Prevalentemente sereno";
            case 2 -> "Parzialmente nuvoloso";
            case 3 -> "Coperto";
            case 45 -> "Nebbia";
            case 48 -> "Nebbia con brina";
            case 51 -> "Pioggerella leggera";
            case 53 -> "Pioggerella moderata";
            case 55 -> "Pioggerella intensa";
            case 56 -> "Pioggerella gelata leggera";
            case 57 -> "Pioggerella gelata intensa";
            case 61 -> "Pioggia leggera";
            case 63 -> "Pioggia moderata";
            case 65 -> "Pioggia intensa";
            case 66 -> "Pioggia gelata leggera";
            case 67 -> "Pioggia gelata intensa";
            case 71 -> "Neve leggera";
            case 73 -> "Neve moderata";
            case 75 -> "Neve intensa";
            case 77 -> "Granelli di neve";
            case 80 -> "Rovesci leggeri";
            case 81 -> "Rovesci moderati";
            case 82 -> "Rovesci violenti";
            case 85 -> "Rovesci di neve leggeri";
            case 86 -> "Rovesci di neve intensi";
            case 95 -> "Temporale";
            case 96 -> "Temporale con grandine leggera";
            case 99 -> "Temporale con grandine intensa";
            default -> "Condizioni sconosciute";
        };
    }

    public String getIcon(int code, boolean isDay) {
        return switch (code) {
            case 0 -> isDay ? "☀️" : "🌙";
            case 1 -> isDay ? "🌤️" : "🌙";
            case 2 -> "⛅";
            case 3 -> "☁️";
            case 45, 48 -> "🌫️";
            case 51, 53, 55, 56, 57 -> "🌦️";
            case 61, 63, 65, 66, 67 -> "🌧️";
            case 71, 73, 75, 77 -> "❄️";
            case 80, 81, 82 -> "🌩️";
            case 85, 86 -> "🌨️";
            case 95, 96, 99 -> "⛈️";
            default -> "🌡️";
        };
    }

    public String getBackgroundClass(int code, boolean isDay) {
        if (!isDay) return "bg-night";
        return switch (code) {
            case 0, 1 -> "bg-sunny";
            case 2, 3 -> "bg-cloudy";
            case 45, 48 -> "bg-foggy";
            case 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "bg-rainy";
            case 71, 73, 75, 77, 85, 86 -> "bg-snowy";
            case 95, 96, 99 -> "bg-stormy";
            default -> "bg-default";
        };
    }

    public String getWindDirection(int degrees) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO"};
        int index = (int) Math.round(degrees / 22.5) % 16;
        return directions[index];
    }
}
