package com.masson.cruciblecraft.benchmark.recipe;

import java.lang.reflect.Array;
import java.util.Map;

/** Small deterministic JSON encoder so the benchmark source set stays isolated. */
final class JsonEncoder {
    private JsonEncoder() {}

    static String encode(Object value) {
        StringBuilder output = new StringBuilder();
        append(output, value, 0);
        output.append('\n');
        return output.toString();
    }

    private static void append(StringBuilder output, Object value, int depth) {
        if (value == null) {
            output.append("null");
        } else if (value instanceof String text) {
            appendString(output, text);
        } else if (value instanceof Boolean || value instanceof Integer
                || value instanceof Long || value instanceof Short
                || value instanceof Byte) {
            output.append(value);
        } else if (value instanceof Float number) {
            finite(number.doubleValue());
            output.append(number);
        } else if (value instanceof Double number) {
            finite(number);
            output.append(number);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(output, map, depth);
        } else if (value instanceof Iterable<?> iterable) {
            appendIterable(output, iterable, depth);
        } else if (value.getClass().isArray()) {
            appendArray(output, value, depth);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported JSON value " + value.getClass().getName());
        }
    }

    private static void appendMap(
            StringBuilder output,
            Map<?, ?> map,
            int depth) {
        output.append('{');
        if (!map.isEmpty()) {
            output.append('\n');
        }
        int index = 0;
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("JSON object key is not text");
            }
            indent(output, depth + 1);
            appendString(output, key);
            output.append(": ");
            append(output, entry.getValue(), depth + 1);
            output.append(++index == map.size() ? '\n' : ',').append(
                    index == map.size() ? "" : "\n");
        }
        if (!map.isEmpty()) {
            indent(output, depth);
        }
        output.append('}');
    }

    private static void appendIterable(
            StringBuilder output,
            Iterable<?> iterable,
            int depth) {
        java.util.List<?> values = iterable instanceof java.util.List<?> list
                ? list : java.util.stream.StreamSupport.stream(
                        iterable.spliterator(), false).toList();
        output.append('[');
        if (!values.isEmpty()) {
            output.append('\n');
        }
        for (int index = 0; index < values.size(); index++) {
            indent(output, depth + 1);
            append(output, values.get(index), depth + 1);
            output.append(index + 1 == values.size() ? '\n' : ',').append(
                    index + 1 == values.size() ? "" : "\n");
        }
        if (!values.isEmpty()) {
            indent(output, depth);
        }
        output.append(']');
    }

    private static void appendArray(
            StringBuilder output,
            Object array,
            int depth) {
        output.append('[');
        int length = Array.getLength(array);
        if (length > 0) {
            output.append('\n');
        }
        for (int index = 0; index < length; index++) {
            indent(output, depth + 1);
            append(output, Array.get(array, index), depth + 1);
            output.append(index + 1 == length ? '\n' : ',').append(
                    index + 1 == length ? "" : "\n");
        }
        if (length > 0) {
            indent(output, depth);
        }
        output.append(']');
    }

    private static void appendString(StringBuilder output, String value) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format(
                                java.util.Locale.ROOT,
                                "\\u%04x",
                                (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }

    private static void indent(StringBuilder output, int depth) {
        output.append("  ".repeat(depth));
    }

    private static void finite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Non-finite number cannot be encoded as JSON");
        }
    }
}
