package im.turms.turms.config.mongo.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EnumToIntegerConverter implements Converter<Enum<?>, Integer> {
    @Override
    public Integer convert(Enum<?> source) {
        return source.ordinal();
    }
}