package com.heal.doctor.utils;

import java.util.function.Consumer;

public class SetIfNotEmpty {
    public static void setIfNotEmpty(String value, Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value);
        }
    }

}
