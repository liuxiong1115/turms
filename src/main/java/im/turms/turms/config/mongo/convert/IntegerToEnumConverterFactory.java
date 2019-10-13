package im.turms.turms.config.mongo.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum> {
    private static final Map<Class<?>, IntegerToEnumConverter> POOL = new HashMap<>();

    @Override
    @NonNull
    public <T extends Enum> Converter<Integer, T> getConverter(@Nullable Class<T> targetType) {
        Class<?> enumType = targetType;
        while (enumType != null && !enumType.isEnum()) {
            enumType = enumType.getSuperclass();
        }
        if (enumType == null) {
            throw new IllegalArgumentException("The target type does not refer to an enum");
        }
        return POOL.computeIfAbsent(enumType, clazz -> new IntegerToEnumConverter(clazz));
    }
}
