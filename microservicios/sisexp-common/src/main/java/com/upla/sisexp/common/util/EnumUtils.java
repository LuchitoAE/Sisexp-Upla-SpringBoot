package com.upla.sisexp.common.util;

import com.upla.sisexp.common.exception.BusinessException;

public class EnumUtils {

    public static <T extends Enum<T>> T parseSafe(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Valor requerido para " + enumClass.getSimpleName());
        }
        try {
            return Enum.valueOf(enumClass, value.replace(' ', '_').trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Valor no valido '" + value + "' para " + enumClass.getSimpleName());
        }
    }
}
