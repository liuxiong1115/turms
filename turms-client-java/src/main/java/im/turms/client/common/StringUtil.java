package im.turms.client.common;

public class StringUtil {
    private StringUtil() {}

    public static String camelToSnakeCase(String camelcase) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < camelcase.length(); i++) {
            char c = camelcase.charAt(i);
            if (Character.isUpperCase(c)) {
                builder.append(builder.length() != 0 ? '_' : "").append(Character.toLowerCase(c));
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }
}
