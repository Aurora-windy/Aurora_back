package com.aurora.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;

import java.io.IOException;

/**
 * BigNumber serializer — avoids JavaScript precision loss for large integers.
 * <p>
 * When the numeric value exceeds JavaScript's safe integer range
 * ({@code Number.MIN_SAFE_INTEGER} .. {@code Number.MAX_SAFE_INTEGER}),
 * the value is serialized as a JSON String; otherwise it stays as a JSON Number.
 * </p>
 */
public class BigNumberSerializer extends NumberSerializer {

    public static final BigNumberSerializer INSTANCE = new BigNumberSerializer(Number.class);

    /** JS max safe integer: 2^53 - 1 = 9007199254740991 */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    /** JS min safe integer: -(2^53 - 1) = -9007199254740991 */
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;

    public BigNumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        long longValue = value.longValue();
        if (longValue > MIN_SAFE_INTEGER && longValue < MAX_SAFE_INTEGER) {
            // Within JS safe range — emit as JSON number
            super.serialize(value, gen, provider);
        } else {
            // Beyond JS safe range — emit as JSON string to preserve precision
            gen.writeString(value.toString());
        }
    }
}
