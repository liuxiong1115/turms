package im.turms.turms.config.mongo.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class IntegerToEnumConverter<T extends Enum> implements Converter<Integer, Enum> {

    private final Class<T> enumType;

    public IntegerToEnumConverter(Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public Enum convert(Integer source) {
        return enumType.getEnumConstants()[source];
    }
}
