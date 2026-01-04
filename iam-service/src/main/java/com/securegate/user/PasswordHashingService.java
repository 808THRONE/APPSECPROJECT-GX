package com.securegate.user;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class PasswordHashingService {

    private static final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    // Params: iterations=3, memory=65536, parallelism=4
    public static String hashPassword(String password) {
        try {
            return argon2.hash(3, 65536, 4, password.toCharArray());
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }

    public static boolean verifyPassword(String hash, String password) {
        try {
            return argon2.verify(hash, password.toCharArray());
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }
}
