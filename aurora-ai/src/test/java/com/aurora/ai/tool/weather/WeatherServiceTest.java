package com.aurora.ai.tool.weather;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolSchemaGenerator;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WeatherServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingCity_returnsValidationErrorWithoutCallingTransport() {
        WeatherHttpClient client = mock(WeatherHttpClient.class);
        WeatherService service = new WeatherService(client, objectMapper);

        AiToolResult result = service.execute(AiToolRequest.builder().params(Map.of()).build());

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("WEATHER_INVALID_ARGUMENT");
    }

    @Test
    void unknownCity_returnsLocationNotFound() throws Exception {
        WeatherService service = new WeatherService(uri -> "{\"results\":[]}", objectMapper);

        AiToolResult result = service.execute(request("Atlantis"));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("WEATHER_LOCATION_NOT_FOUND");
    }

    @Test
    void upstreamFailure_returnsStableError() throws Exception {
        WeatherService service = new WeatherService(uri -> { throw new IOException("503"); }, objectMapper);

        AiToolResult result = service.execute(request("Beijing"));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("WEATHER_UPSTREAM_ERROR");
        assertThat(result.getErrorMessage()).isEqualTo("Weather service is unavailable");
    }

    @Test
    void validLocationAndCurrentWeather_returnsTraceableData() throws Exception {
        WeatherHttpClient client = new QueueClient(List.of(
                "{\"results\":[{\"name\":\"Beijing\",\"country\":\"China\",\"latitude\":39.9,\"longitude\":116.4}]}",
                "{\"timezone\":\"Asia/Shanghai\",\"current\":{\"time\":\"2026-08-31T12:00\",\"temperature_2m\":28.4,\"relative_humidity_2m\":55,\"apparent_temperature\":29.1,\"weather_code\":1,\"wind_speed_10m\":12.3}}"));
        WeatherService service = new WeatherService(client, objectMapper);

        AiToolResult result = service.execute(request("Beijing"));

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getSummary()).contains("Beijing");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data).containsEntry("timezone", "Asia/Shanghai")
                .containsEntry("temperature", 28.4)
                .containsEntry("source", "Open-Meteo")
                .containsKey("queriedAt");
    }

    @Test
    void malformedWeatherResponse_returnsInvalidResponse() throws Exception {
        WeatherService service = new WeatherService(new QueueClient(List.of(
                "{\"results\":[{\"name\":\"Beijing\",\"latitude\":39.9,\"longitude\":116.4}]}",
                "{\"timezone\":\"Asia/Shanghai\"}")), objectMapper);

        AiToolResult result = service.execute(request("Beijing"));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("WEATHER_INVALID_RESPONSE");
    }

    @Test
    void toolDefinition_isReadOnlyAndHasRequiredCitySchema() {
        WeatherToolConfig config = new WeatherToolConfig();
        WeatherService handler = mock(WeatherService.class);
        AiToolDefinition definition = config.weatherCurrentToolDefinition(handler);
        Map<String, Object> generated = new AiToolSchemaGenerator(objectMapper).generate(definition);

        assertThat(definition.getName()).isEqualTo("weather.current");
        assertThat(definition.getMutation()).isFalse();
        assertThat(generated.get("function").toString()).contains("weather__current");
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) generated.get("function");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        assertThat((List<String>) parameters.get("required")).containsExactly("city");
    }

    private static AiToolRequest request(String city) {
        return AiToolRequest.builder().params(Map.of("city", city)).build();
    }

    private static final class QueueClient implements WeatherHttpClient {
        private final java.util.Iterator<String> responses;
        private QueueClient(List<String> responses) { this.responses = responses.iterator(); }
        @Override
        public String get(String uri) {
            if (!responses.hasNext()) throw new IllegalStateException("unexpected request");
            return responses.next();
        }
    }
}
