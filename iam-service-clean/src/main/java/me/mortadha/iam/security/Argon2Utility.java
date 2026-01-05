package me.mortadha.iam.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Argon2id password hashing utility
 * Thread-safe singleton
 */
public class Argon2Utility {

    private static final int SALT_LENGTH;
    private static final int HASH_LENGTH;
    private static final int ITERATIONS;
    private static final int MEMORY;
    private static final int THREADS;
    private static final Argon2 ARGON2;

    static {
        var config = ConfigProvider.getConfig();
        SALT_LENGTH = config.getValue("argon2.saltLength", Integer.class);
        HASH_LENGTH = config.getValue("argon2.hashLength", Integer.class);
        ITERATIONS = config.getValue("argon2.iterations", Integer.class);
        MEMORY = config.getValue("argon2.memory", Integer.class);
        THREADS = config.getValue("argon2.threads", Integer.class);
        
        ARGON2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            SALT_LENGTH,
            HASH_LENGTH
        );
    }

    private Argon2Utility() {
        // Utility class
    }

    /**
     * Hash a password using Argon2id
     * @param password plaintext password
     * @return Argon2id hash string
     */
    public static String hash(char[] password) {
        try {
            return ARGON2.hash(ITERATIONS, MEMORY, THREADS, password);
        } finally {
            ARGON2.wipeArray(password);
        }
    }

    /**
     * Verify a password against an Argon2id hash
     * @param hash the stored hash
     * @param password the password to verify
     * @return true if password matches
     */
    public static boolean verify(String hash, char[] password) {
        try {
            return ARGON2.verify(hash, password);
        } finally {
            ARGON2.wipeArray(password);
        }
    }

    /**
     * Hash a password from String (convenience method)
     */
    public static String hashFromString(String password) {
        return hash(password.toCharArray());
    }

    /**
     * Verify a password from String (convenience method)
     */
    public static boolean verifyFromString(String hash, String password) {
        return verify(hash, password.toCharArray());
    }
}
