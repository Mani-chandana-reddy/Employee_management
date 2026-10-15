package com.yourname.ems.model;

import java.util.Arrays;

import com.yourname.ems.exception.InvalidRequestException;

public enum Department {
    HR, IT, FINANCE, SALES, OPERATIONS;

    /** Case-insensitive lookup that fails with a 400-mapped exception. */
    public static Department from(String value) {
        return Arrays.stream(values())
                .filter(d -> d.name().equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Invalid department '" + value + "'. Allowed values: " + Arrays.toString(values())));
    }
}
