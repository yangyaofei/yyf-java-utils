package com.lingjoin.secret;

import lombok.SneakyThrows;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Secret utils
 */
@SuppressWarnings({"unused"})

public class SecretUtils {
    /**
     * 随机密码生成
     *
     * @param bit the bit
     * @return the string
     */
    @SneakyThrows
    public static String genSecret(int bit) {
        byte[] byteSecret = new byte[bit];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(byteSecret);
        BigInteger bigInteger = new BigInteger(byteSecret).abs();
        return bigInteger.toString(16);
    }
}
