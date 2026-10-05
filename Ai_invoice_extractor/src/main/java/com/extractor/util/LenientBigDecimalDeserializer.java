package com.extractor.util;

import java.math.BigDecimal;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads amounts like 805000.00, "8,05,000.00" or "₹8,05,000.00" as a BigDecimal.
 * The AI sometimes ignores the "plain numbers only" rule, so we clean the text here.
 */
public class LenientBigDecimalDeserializer extends ValueDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        String text = parser.getString();
        if (text == null) {
            return null;
        }
        String cleaned = text.replaceAll("(?i)INR|Rs\\.?|₹", "").replaceAll("[,\\s]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw context.weirdStringException(text, BigDecimal.class, "Not a valid amount");
        }
    }
}