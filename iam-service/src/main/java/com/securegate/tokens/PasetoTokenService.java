public class PasetoService {

    private static final String PRIVATE_KEY = "your-private-key-here";

    public static String createAccessToken(String userId) {
        return Paseto.V4.LOCAL.builder()
                .setSubject(userId)
                .setExpiration(ZonedDateTime.now().plusHours(1))
                .claim("role", "admin")
                .compact();
    }
}
