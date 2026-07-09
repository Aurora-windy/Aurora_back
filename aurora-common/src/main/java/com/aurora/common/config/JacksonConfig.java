package com.aurora.common.config;

import com.aurora.common.serializer.BigNumberSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Global Jackson configuration.
 * <p>
 * Handles two concerns:
 * <ol>
 *   <li><b>Big-number serialization</b> — prevents JavaScript precision loss for
 *       {@code Long} / {@code long} / {@code BigInteger} values that exceed
 *       {@code Number.MAX_SAFE_INTEGER} (2^53 - 1).</li>
 *   <li><b>Java 8 date/time formatting</b> — ensures {@code LocalDateTime},
 *       {@code LocalDate}, {@code LocalTime}, and {@code Instant} are
 *       serialized in a human-readable format instead of timestamps.</li>
 * </ol>
 * <p>
 * Two modes are supported, controlled by {@code aurora.jackson.big-number-mode}:
 * <ul>
 *   <li>{@code flexible} (default) — only values outside JS safe range are
 *       converted to String; small values stay as JSON numbers.</li>
 *   <li>{@code string} — all Long / BigInteger values are converted to String
 *       unconditionally.</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Value("${aurora.jackson.big-number-mode:flexible}")
    private String bigNumberMode;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // ---- 1. Big-number module ----
            if ("string".equalsIgnoreCase(bigNumberMode)) {
                // TO_STRING mode: every Long / BigInteger → String
                builder.serializerByType(Long.class, ToStringSerializer.instance);
                builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
                builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
            } else {
                // FLEXIBLE mode (default): only out-of-range values → String
                builder.serializerByType(Long.class, BigNumberSerializer.INSTANCE);
                builder.serializerByType(Long.TYPE, BigNumberSerializer.INSTANCE);
                builder.serializerByType(BigInteger.class, BigNumberSerializer.INSTANCE);
            }

            // ---- 2. Java 8 date/time module ----
            builder.timeZone(TimeZone.getDefault());
            builder.modules(javaTimeModule());
        };
    }

    private JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFmt));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFmt));

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFmt));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFmt));

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFmt));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFmt));

        module.addSerializer(Instant.class, InstantSerializer.INSTANCE);

        return module;
    }
}
