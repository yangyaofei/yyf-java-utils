package com.lingjoin.cryptoutils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Crypto utils for CharChar20-Poly1305
 */
@SuppressWarnings("unused")
public class CryptoUtils {

    private final static String CHACHA20_POLY_1305 = "ChaCha20-Poly1305";
    private final static String CHACHA20 = "ChaCha20";

    /**
     * Encrypt a InputStream with key and nonce with ChaCha20-Poly1305
     *
     * @param data  the data
     * @param key   the key 256bit
     * @param nonce the nonce 12byte
     * @return the input stream
     * @throws NoSuchPaddingException             the no such padding exception
     * @throws NoSuchAlgorithmException           the no such algorithm exception
     * @throws InvalidAlgorithmParameterException the invalid algorithm parameter exception
     * @throws InvalidKeyException                the invalid key exception
     */
    public static InputStream encrypt(InputStream data, SecretKey key, byte[] nonce) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        return crypt(data, key, nonce, CHACHA20_POLY_1305, Cipher.ENCRYPT_MODE);
    }

    /**
     * Encrypt a InputStream with key and nonce with ChaCha20-Poly1305
     * <p>
     * Build SecretKey with key
     *
     * @param data  the data
     * @param key   the key 256bit
     * @param nonce the nonce 12byte
     * @return the input stream
     * @throws NoSuchAlgorithmException           the no such algorithm exception
     * @throws InvalidAlgorithmParameterException the invalid algorithm parameter exception
     * @throws NoSuchPaddingException             the no such padding exception
     * @throws InvalidKeyException                the invalid key exception
     */
    public static InputStream encrypt(InputStream data, byte[] key, byte[] nonce) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, CHACHA20_POLY_1305);
        return encrypt(data, secretKeySpec, nonce);
    }

    /**
     * Encrypt a InputStream with key and nonce with ChaCha20-Poly1305
     * <p>
     * The key and nonce must be hex string, this is a convince method to use hex string key and nonce
     * <p>
     * The key and nonce:  key {@code ->} deHex {@code ->} getByte, deHex and enHex can use {@link #deHex} and {@link #enHex(byte[])}
     * <p>
     * Key and nonce generation should use {@link #generateKey(String)} and {@link #generateNonce(String)} which will
     * padding the string with space and trim the bytes from string with given length that ChaCha20-Ploy1305 needs.
     *
     * @param data  the data
     * @param key   the key with hex encoded
     * @param nonce the nonce with hex encoded
     * @return the input stream
     * @throws NoSuchAlgorithmException           the no such algorithm exception
     * @throws InvalidAlgorithmParameterException the invalid algorithm parameter exception
     * @throws NoSuchPaddingException             the no such padding exception
     * @throws InvalidKeyException                the invalid key exception
     * @throws DecoderException                   the decoder exception, if the key or nonce is not hex encoded string
     */
    public static InputStream encrypt(InputStream data, String key, String nonce) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, DecoderException {
        return encrypt(data, deHex(key), deHex(nonce));
    }


    /**
     * Decrypt a InputStream with key and nonce with ChaCha20-Poly1305 same as {@link #encrypt(InputStream, SecretKey, byte[])}
     *
     * @param data  the data
     * @param key   the key 256bit
     * @param nonce the nonce 12byte
     * @return the input stream
     * @throws NoSuchPaddingException             the no such padding exception
     * @throws NoSuchAlgorithmException           the no such algorithm exception
     * @throws InvalidAlgorithmParameterException the invalid algorithm parameter exception
     * @throws InvalidKeyException                the invalid key exception
     */
    public static InputStream decrypt(InputStream data, SecretKey key, byte[] nonce) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        return crypt(data, key, nonce, CHACHA20_POLY_1305, Cipher.DECRYPT_MODE);
    }

    /**
     * Decrypt a InputStream with key and nonce with ChaCha20-Poly1305 same as {@link #encrypt(InputStream, byte[], byte[])}
     *
     * @param data  the data
     * @param key   the key 256bit
     * @param nonce the nonce 12byte
     * @return the input stream
     * @throws NoSuchAlgorithmException           the no such algorithm exception
     * @throws InvalidAlgorithmParameterException the invalid algorithm parameter exception
     * @throws NoSuchPaddingException             the no such padding exception
     * @throws InvalidKeyException                the invalid key exception
     */
    public static InputStream decrypt(InputStream data, byte[] key, byte[] nonce) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, CHACHA20_POLY_1305);
        return encrypt(data, secretKeySpec, nonce);
    }

    /**
     * Decrypt a InputStream with key and nonce with ChaCha20-Poly1305 same as {@link #encrypt(InputStream, String, String)}
     *
     * @param data  the data
     * @param key   the key
     * @param nonce the nonce
     * @return the input stream
     * @throws NoSuchAlgorithmException           the no such algorithm exception
     * @throws InvalidAlgorithmParameterException the invalid algorithm parameter exception
     * @throws NoSuchPaddingException             the no such padding exception
     * @throws InvalidKeyException                the invalid key exception
     * @throws DecoderException                   the decoder exception
     */
    public static InputStream decrypt(InputStream data, String key, String nonce) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, DecoderException {
        return encrypt(data, deHex(key), deHex(nonce));
    }


    @SuppressWarnings("SameParameterValue")
    private static InputStream crypt(InputStream data, SecretKey key, byte[] nonce, String alg, int opMode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(alg);
        cipher.init(opMode, key, new IvParameterSpec(nonce));
        return new CipherInputStream(data, cipher);
    }

    /**
     * Generate key with given string.
     * <p>
     * Because the key should be 256bit length, this method will pad the string with space and trim to 256bit in
     * byte array level,  and encode the byte array to a hex string in the end for human reading convince.
     * <p>
     * pass {@code ->} padding with space: pass+ " "*32 {@code ->} trim pass.getByte().trim(256bit) {@code ->} encode hex string
     *
     * @param pass the pass
     * @return the string
     */
    public static String generateKey(String pass) {
        return enHex(padding(pass, 32, ' '));
    }

    /**
     * Generate nonce with given string, same as {@link #generateKey(String)}
     *
     * @param nonce the nonce
     * @return the string
     */
    public static String generateNonce(String nonce) {
        return enHex(padding(nonce, 12, ' '));
    }

    /**
     * Padding string with given char and trim with given length
     *
     * @param data the data
     * @param len  the len
     * @param pad  the pad
     * @return the byte [ ]
     */
    public static byte[] padding(String data, int len, char pad) {
        data += StringUtils.repeat(pad, len);
        byte[] padded = new byte[len];
        System.arraycopy(data.getBytes(), 0, padded, 0, len);
        return padded;
    }

    /**
     * Generate random key and return key as hex encoded string
     *
     * @return the key with hex encoded
     * @throws NoSuchAlgorithmException the no such algorithm exception
     */
    public static String generateRandomKey() throws NoSuchAlgorithmException {
        KeyGenerator instance = KeyGenerator.getInstance(CHACHA20);
        instance.init(256);
        var key = instance.generateKey();
        return enHex(key.getEncoded());
    }

    /**
     * Encode a byte array to hex string.
     *
     * @param binary the binary
     * @return the string
     */
    public static String enHex(byte[] binary) {
        return Hex.encodeHexString(binary);
    }

    /**
     * Decode a hex string to a byte array.
     *
     * @param hexString the hex string
     * @return the byte [ ]
     * @throws DecoderException the decoder exception
     */
    public static byte[] deHex(String hexString) throws DecoderException {
        return Hex.decodeHex(hexString);
    }
}
