package com.weather.app.controller;

import com.weather.app.model.GeoLocation;
import com.weather.app.model.WeatherViewModel;
import com.weather.app.service.GeocodingService;
import com.weather.app.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    private final GeocodingService geocodingService;
    private final WeatherService weatherService;

    public WeatherController(GeocodingService geocodingService, WeatherService weatherService) {
        this.geocodingService = geocodingService;
        this.weatherService = weatherService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "Meteo - Cerca una città");
        return "index";
    }

    @GetMapping("/weather")
    public String weather(@RequestParam("city") String city,
                          @RequestParam(value = "lat", required = false) Double lat,
                          @RequestParam(value = "lon", required = false) Double lon,
                          Model model) {

        if (city == null || city.isBlank()) {
            model.addAttribute("error", "Inserisci il nome di una città.");
            return "index";
        }

        try {
            GeoLocation location = geocodingService.findCity(city, lat, lon);
            if (location == null) {
                model.addAttribute("error", "Città non trovata: " + city);
                model.addAttribute("title", "Meteo - Città non trovata");
                return "index";
            }

            WeatherViewModel vm = weatherService.getWeather(location);
            model.addAttribute("weather", vm);
            model.addAttribute("title", "Meteo - " + vm.getCityDisplayName());
            return "weather";

        } catch (IOException e) {
            log.error("Error fetching weather for {}", city, e);
            model.addAttribute("error", "Errore nel recupero dei dati meteo. Riprova tra poco.");
            model.addAttribute("title", "Meteo - Errore");
            return "index";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            model.addAttribute("error", "Richiesta interrotta. Riprova.");
            return "index";
        }
    }

    @GetMapping("/api/cities")
    @ResponseBody
    public List<Map<String, Object>> searchCities(@RequestParam("q") String query) {
        List<GeoLocation> locations = geocodingService.searchCities(query, 8);
        return locations.stream().map(loc -> Map.<String, Object>of(
                "name", loc.getName(),
                "displayName", loc.getDisplayName(),
                "lat", loc.getLatitude(),
                "lon", loc.getLongitude(),
                "country", loc.getCountry() != null ? loc.getCountry() : "",
                "countryCode", loc.getCountryCode() != null ? loc.getCountryCode() : ""
        )).collect(Collectors.toList());
    }
}
