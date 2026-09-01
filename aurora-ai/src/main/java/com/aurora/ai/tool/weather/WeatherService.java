package com.aurora.ai.tool.weather;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WeatherService implements AiToolHandler {
    private static final String GEOCODING_ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_ENDPOINT = "https://api.open-meteo.com/v1/forecast";
    private static final String CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m";

    private final WeatherHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        String city = textParam(request, "city");
        String country = textParam(request, "country");
        if (!StringUtils.hasText(city)) {
            return AiToolResult.fail("WEATHER_INVALID_ARGUMENT", "city is required");
        }
        String locationQuery = StringUtils.hasText(country) ? city.trim() + ", " + country.trim() : city.trim();
        try {
            JsonNode location = findLocation(locationQuery);
            if (location == null) {
                return AiToolResult.fail("WEATHER_LOCATION_NOT_FOUND", "No location found for: " + locationQuery);
            }
            double latitude = requiredDouble(location, "latitude");
            double longitude = requiredDouble(location, "longitude");
            JsonNode weather;
            try {
                weather = objectMapper.readTree(httpClient.get(weatherUri(latitude, longitude)));
            } catch (JsonProcessingException ex) {
                throw new WeatherServiceException("WEATHER_INVALID_RESPONSE", "Weather response was not valid JSON");
            }
            return buildResult(location, weather);
        } catch (WeatherServiceException ex) {
            return AiToolResult.fail(ex.code, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return AiToolResult.fail("WEATHER_NETWORK_ERROR", "Weather service request was interrupted");
        } catch (IOException | RuntimeException ex) {
            return AiToolResult.fail("WEATHER_UPSTREAM_ERROR", "Weather service is unavailable");
        }
    }

    private JsonNode findLocation(String query) throws IOException, InterruptedException {
        String json = httpClient.get(GEOCODING_ENDPOINT + "?name=" + encode(query) + "&count=1&language=en&format=json");
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new WeatherServiceException("WEATHER_INVALID_RESPONSE", "Geocoding response was not valid JSON");
        }
        JsonNode results = root == null ? null : root.get("results");
        return results != null && results.isArray() && !results.isEmpty() ? results.get(0) : null;
    }

    private AiToolResult buildResult(JsonNode location, JsonNode weather) {
        JsonNode current = weather == null ? null : weather.get("current");
        if (current == null || !current.isObject()) {
            throw new WeatherServiceException("WEATHER_INVALID_RESPONSE", "Weather response did not contain current data");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("city", text(location, "name"));
        putIfPresent(data, "country", text(location, "country"));
        data.put("latitude", requiredDouble(location, "latitude"));
        data.put("longitude", requiredDouble(location, "longitude"));
        putIfPresent(data, "timezone", text(weather, "timezone"));
        putIfPresent(data, "localTime", text(current, "time"));
        putIfPresent(data, "temperature", number(current, "temperature_2m"));
        putIfPresent(data, "apparentTemperature", number(current, "apparent_temperature"));
        putIfPresent(data, "relativeHumidity", number(current, "relative_humidity_2m"));
        putIfPresent(data, "windSpeed", number(current, "wind_speed_10m"));
        putIfPresent(data, "weatherCode", number(current, "weather_code"));
        putIfPresent(data, "weatherDescription", weatherDescription(number(current, "weather_code")));
        data.put("source", "Open-Meteo");
        data.put("queriedAt", Instant.now().toString());
        String city = String.valueOf(data.get("city"));
        String temperature = String.valueOf(data.getOrDefault("temperature", "unknown"));
        return AiToolResult.ok(data, "Current weather for " + city + ": " + temperature + " C");
    }

    private String weatherUri(double latitude, double longitude) {
        return WEATHER_ENDPOINT + "?latitude=" + latitude + "&longitude=" + longitude + "&current=" + CURRENT_FIELDS + "&timezone=auto";
    }

    private double requiredDouble(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isNumber()) {
            throw new WeatherServiceException("WEATHER_INVALID_RESPONSE", "Weather response is missing " + field);
        }
        return value.doubleValue();
    }

    private static Number number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.numberValue() : null;
    }

    private static String weatherDescription(Number code) {
        if (code == null) return null;
        return switch (code.intValue()) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Fog";
            case 51, 53, 55, 56, 57 -> "Drizzle";
            case 61, 63, 65, 66, 67 -> "Rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown conditions";
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }

    private static String textParam(AiToolRequest request, String name) {
        if (request == null || request.getParams() == null) return null;
        Object value = request.getParams().get(name);
        return value == null ? null : String.valueOf(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static class WeatherServiceException extends RuntimeException {
        private final String code;
        WeatherServiceException(String code, String message) { super(message); this.code = code; }
    }
}
