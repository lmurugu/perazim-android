package com.example.app.data.local.converter;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Room TypeConverters for persisting complex or date types into SQLite primitives.
 */
public class Converters {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    @TypeConverter
    public static List<String> toStringList(String data) {
        if (data == null || data.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = data.split(",");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(part.trim());
        }
        return result;
    }
}
