package com.aurora.ai.tool.weather;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolDefinition.ParamField;
import com.aurora.ai.tool.core.AiToolDefinition.ParamType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class WeatherToolConfig {
    @Bean
    public AiToolDefinition weatherCurrentToolDefinition(WeatherService handler) {
        return AiToolDefinition.builder()
                .name("weather.current")
                .description("Query the current weather for a city using Open-Meteo. Read-only external data.")
                .mutation(false)
                .paramSchema(List.of(
                        new ParamField("city", ParamType.STRING, true, "City name, for example Beijing"),
                        new ParamField("country", ParamType.STRING, false, "Optional country or region hint")))
                .handler(handler)
                .build();
    }
}
