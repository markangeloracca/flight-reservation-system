package ca.pacific.frs.domain.policy;

import java.security.SecureRandom;
import java.util.Random;

/** Generates 6-character booking references without look-alike characters (no 0/O, 1/I/L). */
public final class ConfirmationCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    private final Random random;

    public ConfirmationCodeGenerator() {
        this(new SecureRandom());
    }

    public ConfirmationCodeGenerator(Random random) {
        this.random = random;
    }

    public String next() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
