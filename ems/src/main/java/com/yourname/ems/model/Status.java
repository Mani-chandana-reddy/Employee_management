package com.yourname.ems.model;

import java.util.Arrays;

import com.yourname.ems.exception.InvalidRequestException;

public enum Status {
    ACTIVE, INACTIVE;

    /** Case-insensitive lookup that fails with a 400-mapped exception. */
    public static Status from(String value) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Invalid status '" + value + "'. Allowed values: " + Arrays.toString(values())));
    }
}
