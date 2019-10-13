package im.turms.turms.common;

import java.io.IOException;

public class JsonUtil {
    public static <T> T deepCopy(T object, Class<T> clazz) throws IOException {
        byte[] bytes = Constants.MAPPER.writeValueAsBytes(object);
        return Constants.MAPPER.readValue(bytes, clazz);
    }
}
