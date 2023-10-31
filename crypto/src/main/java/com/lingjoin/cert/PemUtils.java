/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package com.lingjoin.cert;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.ECKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Pem utils.
 */
@SuppressWarnings("unused")
public class PemUtils {

    private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String OPENSSL_DSA_HEADER = "-----BEGIN DSA PRIVATE KEY-----";
    private static final String OPENSSL_DSA_FOOTER = "-----END DSA PRIVATE KEY-----";
    private static final String OPENSSL_DSA_PARAMS_HEADER = "-----BEGIN DSA PARAMETERS-----";
    private static final String OPENSSL_DSA_PARAMS_FOOTER = "-----END DSA PARAMETERS-----";
    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PKCS8_ENCRYPTED_HEADER = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
    private static final String PKCS8_ENCRYPTED_FOOTER = "-----END ENCRYPTED PRIVATE KEY-----";
    private static final String OPENSSL_EC_HEADER = "-----BEGIN EC PRIVATE KEY-----";
    private static final String OPENSSL_EC_FOOTER = "-----END EC PRIVATE KEY-----";
    private static final String OPENSSL_EC_PARAMS_HEADER = "-----BEGIN EC PARAMETERS-----";
    private static final String OPENSSL_EC_PARAMS_FOOTER = "-----END EC PARAMETERS-----";
    private static final String HEADER = "-----BEGIN";

    private static final String PBES2_OID = "1.2.840.113549.1.5.13";
    private static final String AES_OID = "2.16.840.1.101.3.4.1";

    private PemUtils() {
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    /**
     * Creates a {@link PrivateKey} from the contents of a file. Supports PKCS#1, PKCS#8
     * encoded formats of encrypted and plaintext RSA, DSA and EC(secp256r1) keys
     *
     * @param keyPath          the path for the key file
     * @param passwordSupplier A password supplier for the potentially encrypted (password protected) key
     * @return a private key from the contents of the file
     * @throws IOException if the file can't be read
     */
    public static PrivateKey readPrivateKey(Path keyPath, Supplier<char[]> passwordSupplier) throws IOException {
        try (BufferedReader bReader = Files.newBufferedReader(keyPath, StandardCharsets.UTF_8)) {
            String line = bReader.readLine();
            while (null != line && line.startsWith(HEADER) == false) {
                line = bReader.readLine();
            }
            if (null == line) {
                throw new IllegalStateException("Error parsing Private Key from: " + keyPath.toString() + ". File is empty");
            }
            return switch (line.trim()) {
                case PKCS8_ENCRYPTED_HEADER -> {
                    char[] password = passwordSupplier.get();
                    if (password == null) {
                        throw new IllegalArgumentException("cannot read encrypted key without a password");
                    }
                    yield parsePKCS8Encrypted(bReader, password);
                }
                case PKCS8_HEADER -> parsePKCS8(bReader);
                case PKCS1_HEADER -> parsePKCS1Rsa(bReader, passwordSupplier);
                case OPENSSL_DSA_HEADER -> parseOpenSslDsa(bReader, passwordSupplier);
                case OPENSSL_DSA_PARAMS_HEADER -> parseOpenSslDsa(removeDsaHeaders(bReader), passwordSupplier);
                case OPENSSL_EC_HEADER -> parseOpenSslEC(bReader, passwordSupplier);
                case OPENSSL_EC_PARAMS_HEADER -> parseOpenSslEC(removeECHeaders(bReader), passwordSupplier);
                default -> throw new IllegalStateException(
                        "Error parsing Private Key from: " + keyPath.toString() + ". File did not contain a " + "supported key format"
                );
            };
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error parsing Private Key from: " + keyPath.toString(), e);
        }
    }

    /**
     * Removes the EC Headers that OpenSSL adds to EC private keys as the information in them
     * is redundant
     *
     * @throws IOException if the EC Parameter footer is missing
     */
    private static BufferedReader removeECHeaders(BufferedReader bReader) throws IOException {
        String line = bReader.readLine();
        while (line != null) {
            if (OPENSSL_EC_PARAMS_FOOTER.equals(line.trim())) {
                break;
            }
            line = bReader.readLine();
        }
        if (null == line || OPENSSL_EC_PARAMS_FOOTER.equals(line.trim()) == false) {
            throw new IOException("Malformed PEM file, EC Parameters footer is missing");
        }
        // Verify that the key starts with the correct header before passing it to parseOpenSslEC
        if (OPENSSL_EC_HEADER.equals(bReader.readLine()) == false) {
            throw new IOException("Malformed PEM file, EC Key header is missing");
        }
        return bReader;
    }

    /**
     * Removes the DSA Params Headers that OpenSSL adds to DSA private keys as the information in them
     * is redundant
     *
     * @throws IOException if the EC Parameter footer is missing
     */
    private static BufferedReader removeDsaHeaders(BufferedReader bReader) throws IOException {
        String line = bReader.readLine();
        while (line != null) {
            if (OPENSSL_DSA_PARAMS_FOOTER.equals(line.trim())) {
                break;
            }
            line = bReader.readLine();
        }
        if (null == line || OPENSSL_DSA_PARAMS_FOOTER.equals(line.trim()) == false) {
            throw new IOException("Malformed PEM file, DSA Parameters footer is missing");
        }
        // Verify that the key starts with the correct header before passing it to parseOpenSslDsa
        if (OPENSSL_DSA_HEADER.equals(bReader.readLine()) == false) {
            throw new IOException("Malformed PEM file, DSA Key header is missing");
        }
        return bReader;
    }

    /**
     * Creates a {@link PrivateKey} from the contents of {@code bReader} that contains an plaintext private key encoded in
     * PKCS#8
     *
     * @param bReader the {@link BufferedReader} containing the key file contents
     * @return {@link PrivateKey}
     * @throws IOException              if the file can't be read
     * @throws GeneralSecurityException if the private key can't be generated from the {@link PKCS8EncodedKeySpec}
     */
    private static PrivateKey parsePKCS8(BufferedReader bReader) throws IOException, GeneralSecurityException {
        StringBuilder sb = new StringBuilder();
        String line = bReader.readLine();
        while (line != null) {
            if (PKCS8_FOOTER.equals(line.trim())) {
                break;
            }
            sb.append(line.trim());
            line = bReader.readLine();
        }
        if (null == line || PKCS8_FOOTER.equals(line.trim()) == false) {
            throw new KeyException("Malformed PEM file, PEM footer is invalid or missing");
        }
        byte[] keyBytes = Base64.getDecoder().decode(sb.toString());
        String keyAlgo = getKeyAlgorithmIdentifier(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgo);
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    /**
     * Creates a {@link PrivateKey} from the contents of {@code bReader} that contains an EC private key encoded in
     * OpenSSL traditional format.
     *
     * @param bReader          the {@link BufferedReader} containing the key file contents
     * @param passwordSupplier A password supplier for the potentially encrypted (password protected) key
     * @return {@link PrivateKey}
     * @throws IOException              if the file can't be read
     * @throws GeneralSecurityException if the private key can't be generated from the {@link ECPrivateKeySpec}
     */
    private static PrivateKey parseOpenSslEC(BufferedReader bReader, Supplier<char[]> passwordSupplier) throws IOException,
            GeneralSecurityException {
        StringBuilder sb = new StringBuilder();
        String line = bReader.readLine();
        Map<String, String> pemHeaders = new HashMap<>();
        while (line != null) {
            if (OPENSSL_EC_FOOTER.equals(line.trim())) {
                break;
            }
            // Parse PEM headers according to https://www.ietf.org/rfc/rfc1421.txt
            if (line.contains(":")) {
                String[] header = line.split(":");
                pemHeaders.put(header[0].trim(), header[1].trim());
            } else {
                sb.append(line.trim());
            }
            line = bReader.readLine();
        }
        if (null == line || OPENSSL_EC_FOOTER.equals(line.trim()) == false) {
            throw new IOException("Malformed PEM file, PEM footer is invalid or missing");
        }
        byte[] keyBytes = possiblyDecryptPKCS1Key(pemHeaders, sb.toString(), passwordSupplier);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        ECPrivateKeySpec ecSpec = parseEcDer(keyBytes);
        return keyFactory.generatePrivate(ecSpec);
    }

    /**
     * Creates a {@link PrivateKey} from the contents of {@code bReader} that contains an RSA private key encoded in
     * OpenSSL traditional format.
     *
     * @param bReader          the {@link BufferedReader} containing the key file contents
     * @param passwordSupplier A password supplier for the potentially encrypted (password protected) key
     * @return {@link PrivateKey}
     * @throws IOException              if the file can't be read
     * @throws GeneralSecurityException if the private key can't be generated from the {@link RSAPrivateCrtKeySpec}
     */
    private static PrivateKey parsePKCS1Rsa(BufferedReader bReader, Supplier<char[]> passwordSupplier) throws IOException,
            GeneralSecurityException {
        StringBuilder sb = new StringBuilder();
        String line = bReader.readLine();
        Map<String, String> pemHeaders = new HashMap<>();

        while (line != null) {
            if (PKCS1_FOOTER.equals(line.trim())) {
                // Unencrypted
                break;
            }
            // Parse PEM headers according to https://www.ietf.org/rfc/rfc1421.txt
            if (line.contains(":")) {
                String[] header = line.split(":");
                pemHeaders.put(header[0].trim(), header[1].trim());
            } else {
                sb.append(line.trim());
            }
            line = bReader.readLine();
        }
        if (null == line || PKCS1_FOOTER.equals(line.trim()) == false) {
            throw new IOException("Malformed PEM file, PEM footer is invalid or missing");
        }
        byte[] keyBytes = possiblyDecryptPKCS1Key(pemHeaders, sb.toString(), passwordSupplier);
        RSAPrivateCrtKeySpec spec = parseRsaDer(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Creates a {@link PrivateKey} from the contents of {@code bReader} that contains an DSA private key encoded in
     * OpenSSL traditional format.
     *
     * @param bReader          the {@link BufferedReader} containing the key file contents
     * @param passwordSupplier A password supplier for the potentially encrypted (password protected) key
     * @return {@link PrivateKey}
     * @throws IOException              if the file can't be read
     * @throws GeneralSecurityException if the private key can't be generated from the {@link DSAPrivateKeySpec}
     */
    private static PrivateKey parseOpenSslDsa(BufferedReader bReader, Supplier<char[]> passwordSupplier) throws IOException,
            GeneralSecurityException {
        StringBuilder sb = new StringBuilder();
        String line = bReader.readLine();
        Map<String, String> pemHeaders = new HashMap<>();

        while (line != null) {
            if (OPENSSL_DSA_FOOTER.equals(line.trim())) {
                // Unencrypted
                break;
            }
            // Parse PEM headers according to https://www.ietf.org/rfc/rfc1421.txt
            if (line.contains(":")) {
                String[] header = line.split(":");
                pemHeaders.put(header[0].trim(), header[1].trim());
            } else {
                sb.append(line.trim());
            }
            line = bReader.readLine();
        }
        if (null == line || OPENSSL_DSA_FOOTER.equals(line.trim()) == false) {
            throw new IOException("Malformed PEM file, PEM footer is invalid or missing");
        }
        byte[] keyBytes = possiblyDecryptPKCS1Key(pemHeaders, sb.toString(), passwordSupplier);
        DSAPrivateKeySpec spec = parseDsaDer(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Creates a {@link PrivateKey} from the contents of {@code bReader} that contains an encrypted private key encoded in
     * PKCS#8
     *
     * @param bReader     the {@link BufferedReader} containing the key file contents
     * @param keyPassword The password for the encrypted (password protected) key
     * @return {@link PrivateKey}
     * @throws IOException              if the file can't be read
     * @throws GeneralSecurityException if the private key can't be generated from the {@link PKCS8EncodedKeySpec}
     */
    private static PrivateKey parsePKCS8Encrypted(BufferedReader bReader, char[] keyPassword) throws IOException, GeneralSecurityException {
        StringBuilder sb = new StringBuilder();
        String line = bReader.readLine();
        while (line != null) {
            if (PKCS8_ENCRYPTED_FOOTER.equals(line.trim())) {
                break;
            }
            sb.append(line.trim());
            line = bReader.readLine();
        }
        if (null == line || PKCS8_ENCRYPTED_FOOTER.equals(line.trim()) == false) {
            throw new IOException("Malformed PEM file, PEM footer is invalid or missing");
        }
        byte[] keyBytes = Base64.getDecoder().decode(sb.toString());

        final EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = getEncryptedPrivateKeyInfo(keyBytes);
        String algorithm = encryptedPrivateKeyInfo.getAlgName();
        if (algorithm.equals("PBES2") || algorithm.equals("1.2.840.113549.1.5.13")) {
            algorithm = getPBES2Algorithm(encryptedPrivateKeyInfo);
        }
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
        SecretKey secretKey = secretKeyFactory.generateSecret(new PBEKeySpec(keyPassword));
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, encryptedPrivateKeyInfo.getAlgParameters());
        PKCS8EncodedKeySpec keySpec = encryptedPrivateKeyInfo.getKeySpec(cipher);
        String keyAlgo = getKeyAlgorithmIdentifier(keySpec.getEncoded());
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgo);
        return keyFactory.generatePrivate(keySpec);
    }

    private static EncryptedPrivateKeyInfo getEncryptedPrivateKeyInfo(byte[] keyBytes) throws IOException, GeneralSecurityException {
        try {
            return new EncryptedPrivateKeyInfo(keyBytes);
        } catch (IOException e) {
            // The Sun JCE provider can't handle non-AES PBES2 data (but it can handle PBES1 DES data - go figure)
            // It's not worth our effort to try and decrypt it ourselves, but we can detect it and give a good error message
            DerParser parser = new DerParser(keyBytes);
            final DerParser.Asn1Object rootSeq = parser.readAsn1Object(DerParser.SEQUENCE);
            parser = rootSeq.getParser();
            final DerParser.Asn1Object algSeq = parser.readAsn1Object();
            parser = algSeq.getParser();
            final String algId = parser.readAsn1Object().getOid();
            if (PBES2_OID.equals(algId)) {
                final DerParser.Asn1Object algData = parser.readAsn1Object(DerParser.SEQUENCE);
                parser = algData.getParser();
                final DerParser.Asn1Object ignoreKdf = parser.readAsn1Object(DerParser.SEQUENCE);
                final DerParser.Asn1Object cryptSeq = parser.readAsn1Object(DerParser.SEQUENCE);
                parser = cryptSeq.getParser();
                final String encryptionId = parser.readAsn1Object(DerParser.OBJECT_OID).getOid();
                if (!encryptionId.startsWith(AES_OID)) {
                    final String name = getAlgorithmNameFromOid(encryptionId);
                    throw new GeneralSecurityException(
                            "PKCS#8 Private Key is encrypted with unsupported PBES2 algorithm ["
                                    + encryptionId
                                    + "]"
                                    + (name == null ? "" : " (" + name + ")"),
                            e
                    );
                }
                /*if (JavaVersion.current().compareTo(JavaVersion.parse("11.0.0")) < 0) {
                    // PBES2 appears to be supported on Oracle 8, but not OpenJDK8
                    // We don't bother clarifying the distinction here, because it's complicated and the best advice we can give is to
                    // use the bundled JDK.
                    throw new GeneralSecurityException(
                        "PKCS#8 Private Key is encrypted with PBES2 which is not supported on this JDK ["
                            + JavaVersion.current()
                            + "], this problem can be resolved by using the Elasticsearch bundled JDK",
                        e
                    );
                }*/
            }
            throw e;
        }
    }

    /**
     * This is horrible, but it's the only option other than to parse the encoded ASN.1 value ourselves
     *
     * @see AlgorithmParameters#toString() and com.sun.crypto.provider.PBES2Parameters#toString()
     */
    private static String getPBES2Algorithm(EncryptedPrivateKeyInfo encryptedPrivateKeyInfo) {
        final AlgorithmParameters algParameters = encryptedPrivateKeyInfo.getAlgParameters();
        if (algParameters != null) {
            return algParameters.toString();
        } else {
            // AlgorithmParameters can be null when running on BCFIPS.
            // However, since BCFIPS doesn't support any PBE specs, nothing we do here would work, so we just do enough to avoid an NPE
            return encryptedPrivateKeyInfo.getAlgName();
        }
    }

    /**
     * Decrypts the password protected contents using the algorithm and IV that is specified in the PEM Headers of the file
     *
     * @param pemHeaders       The Proc-Type and DEK-Info PEM headers that have been extracted from the key file
     * @param keyContents      The key as a base64 encoded String
     * @param passwordSupplier A password supplier for the encrypted (password protected) key
     * @return the decrypted key bytes
     * @throws GeneralSecurityException if the key can't be decrypted
     * @throws IOException              if the PEM headers are missing or malformed
     */
    private static byte[] possiblyDecryptPKCS1Key(Map<String, String> pemHeaders, String keyContents, Supplier<char[]> passwordSupplier)
            throws GeneralSecurityException, IOException {
        byte[] keyBytes = Base64.getDecoder().decode(keyContents);
        String procType = pemHeaders.get("Proc-Type");
        if ("4,ENCRYPTED".equals(procType)) {
            // We only handle PEM encryption
            String encryptionParameters = pemHeaders.get("DEK-Info");
            if (null == encryptionParameters) {
                // malformed pem
                throw new IOException("Malformed PEM File, DEK-Info header is missing");
            }
            char[] password = passwordSupplier.get();
            if (password == null) {
                throw new IOException("cannot read encrypted key without a password");
            }
            Cipher cipher = getCipherFromParameters(encryptionParameters, password);
            byte[] decryptedKeyBytes = cipher.doFinal(keyBytes);
            return decryptedKeyBytes;
        }
        return keyBytes;
    }

    /**
     * Creates a {@link Cipher} from the contents of the DEK-Info header of a PEM file. RFC 1421 indicates that supported algorithms are
     * defined in RFC 1423. RFC 1423 only defines DES-CBS and triple DES (EDE) in CBC mode. AES in CBC mode is also widely used though ( 3
     * different variants of 128, 192, 256 bit keys )
     *
     * @param dekHeaderValue The value of the DEK-Info PEM header
     * @param password       The password with which the key is encrypted
     * @return a cipher of the appropriate algorithm and parameters to be used for decryption
     * @throws GeneralSecurityException if the algorithm is not available in the used security provider, or if the key is inappropriate
     *                                  for the cipher
     * @throws IOException              if the DEK-Info PEM header is invalid
     */
    private static Cipher getCipherFromParameters(String dekHeaderValue, char[] password) throws GeneralSecurityException, IOException {
        String padding = "PKCS5Padding";
        SecretKey encryptionKey;
        String[] valueTokens = dekHeaderValue.split(",");
        if (valueTokens.length != 2) {
            throw new IOException("Malformed PEM file, DEK-Info PEM header is invalid");
        }
        String algorithm = valueTokens[0];
        String ivString = valueTokens[1];
        byte[] iv = hexStringToByteArray(ivString);
        if ("DES-CBC".equals(algorithm)) {
            byte[] key = generateOpenSslKey(password, iv, 8);
            encryptionKey = new SecretKeySpec(key, "DES");
        } else if ("DES-EDE3-CBC".equals(algorithm)) {
            byte[] key = generateOpenSslKey(password, iv, 24);
            encryptionKey = new SecretKeySpec(key, "DESede");
        } else if ("AES-128-CBC".equals(algorithm)) {
            byte[] key = generateOpenSslKey(password, iv, 16);
            encryptionKey = new SecretKeySpec(key, "AES");
        } else if ("AES-192-CBC".equals(algorithm)) {
            byte[] key = generateOpenSslKey(password, iv, 24);
            encryptionKey = new SecretKeySpec(key, "AES");
        } else if ("AES-256-CBC".equals(algorithm)) {
            byte[] key = generateOpenSslKey(password, iv, 32);
            encryptionKey = new SecretKeySpec(key, "AES");
        } else {
            throw new GeneralSecurityException("Private Key encrypted with unsupported algorithm: " + algorithm);
        }
        String transformation = encryptionKey.getAlgorithm() + "/" + "CBC" + "/" + padding;
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new IvParameterSpec(iv));
        return cipher;
    }

    /**
     * Performs key stretching in the same manner that OpenSSL does. This is basically a KDF
     * that uses n rounds of salted MD5 (as many times as needed to get the necessary number of key bytes)
     * <p>
     * https://www.openssl.org/docs/man1.1.0/crypto/PEM_write_bio_PrivateKey_traditional.html
     *
     * @param password The password to use for key stretching.
     * @param salt     The salt to use for key stretching.
     * @param keyLength The desired length of the key in bytes.
     * @return The generated key.
     */
    @SneakyThrows
    private static byte[] generateOpenSslKey(char[] password, byte[] salt, int keyLength) {
        byte[] passwordBytes = PemUtils.toUtf8Bytes(password);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] key = new byte[keyLength];
        int copied = 0;
        int remaining;
        while (copied < keyLength) {
            remaining = keyLength - copied;
            md5.update(passwordBytes, 0, passwordBytes.length);
            md5.update(salt, 0, 8);// AES IV (salt) is longer but we only need 8 bytes
            byte[] tempDigest = md5.digest();
            int bytesToCopy = Math.min(remaining, 16); // MD5 digests are 16 bytes
            System.arraycopy(tempDigest, 0, key, copied, bytesToCopy);
            copied += bytesToCopy;
            if (remaining == 0) {
                break;
            }
            md5.update(tempDigest, 0, 16); // use previous round digest as IV
        }
        Arrays.fill(passwordBytes, (byte) 0);
        return key;
    }

    /**
     * Converts a hexadecimal string to a byte array
     */
    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        if (len % 2 == 0) {
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                final int k = Character.digit(hexString.charAt(i), 16);
                final int l = Character.digit(hexString.charAt(i + 1), 16);
                if (k == -1 || l == -1) {
                    throw new IllegalStateException("String is not hexadecimal");
                }
                data[i / 2] = (byte) ((k << 4) + l);
            }
            return data;
        } else {
            throw new IllegalStateException("Hexadeciamal string length is odd, can't convert to byte array");
        }
    }

    /**
     * Parses a DER encoded EC key to an {@link ECPrivateKeySpec} using a minimal {@link DerParser}
     *
     * @param keyBytes the private key raw bytes
     * @return {@link ECPrivateKeySpec}
     * @throws IOException if the DER encoded key can't be parsed
     */
    private static ECPrivateKeySpec parseEcDer(byte[] keyBytes) throws IOException, GeneralSecurityException {
        DerParser parser = new DerParser(keyBytes);
        DerParser.Asn1Object sequence = parser.readAsn1Object();
        parser = sequence.getParser();
        parser.readAsn1Object().getInteger(); // version
        String keyHex = parser.readAsn1Object().getString();
        BigInteger privateKeyInt = new BigInteger(keyHex, 16);
        DerParser.Asn1Object choice = parser.readAsn1Object();
        parser = choice.getParser();
        String namedCurve = getEcCurveNameFromOid(parser.readAsn1Object().getOid());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        AlgorithmParameterSpec algorithmParameterSpec = new ECGenParameterSpec(namedCurve);
        keyPairGenerator.initialize(algorithmParameterSpec);
        ECParameterSpec parameterSpec = ((ECKey) keyPairGenerator.generateKeyPair().getPrivate()).getParams();
        return new ECPrivateKeySpec(privateKeyInt, parameterSpec);
    }

    /**
     * Parses a DER encoded RSA key to a {@link RSAPrivateCrtKeySpec} using a minimal {@link DerParser}
     *
     * @param keyBytes the private key raw bytes
     * @return {@link RSAPrivateCrtKeySpec}
     * @throws IOException if the DER encoded key can't be parsed
     */
    private static RSAPrivateCrtKeySpec parseRsaDer(byte[] keyBytes) throws IOException {
        DerParser parser = new DerParser(keyBytes);
        DerParser.Asn1Object sequence = parser.readAsn1Object();
        parser = sequence.getParser();
        parser.readAsn1Object().getInteger(); // (version) We don't need it but must read to get to modulus
        BigInteger modulus = parser.readAsn1Object().getInteger();
        BigInteger publicExponent = parser.readAsn1Object().getInteger();
        BigInteger privateExponent = parser.readAsn1Object().getInteger();
        BigInteger prime1 = parser.readAsn1Object().getInteger();
        BigInteger prime2 = parser.readAsn1Object().getInteger();
        BigInteger exponent1 = parser.readAsn1Object().getInteger();
        BigInteger exponent2 = parser.readAsn1Object().getInteger();
        BigInteger coefficient = parser.readAsn1Object().getInteger();
        return new RSAPrivateCrtKeySpec(modulus, publicExponent, privateExponent, prime1, prime2, exponent1, exponent2, coefficient);
    }

    /**
     * Parses a DER encoded DSA key to a {@link DSAPrivateKeySpec} using a minimal {@link DerParser}
     *
     * @param keyBytes the private key raw bytes
     * @return {@link DSAPrivateKeySpec}
     * @throws IOException if the DER encoded key can't be parsed
     */
    private static DSAPrivateKeySpec parseDsaDer(byte[] keyBytes) throws IOException {
        DerParser parser = new DerParser(keyBytes);
        DerParser.Asn1Object sequence = parser.readAsn1Object();
        parser = sequence.getParser();
        parser.readAsn1Object().getInteger(); // (version) We don't need it but must read to get to p
        BigInteger p = parser.readAsn1Object().getInteger();
        BigInteger q = parser.readAsn1Object().getInteger();
        BigInteger g = parser.readAsn1Object().getInteger();
        parser.readAsn1Object().getInteger(); // we don't need x
        BigInteger x = parser.readAsn1Object().getInteger();
        return new DSAPrivateKeySpec(x, p, q, g);
    }

    /**
     * Parses a DER encoded private key and reads its algorithm identifier Object OID.
     *
     * @param keyBytes the private key raw bytes
     * @return A string identifier for the key algorithm (RSA, DSA, or EC)
     * @throws GeneralSecurityException if the algorithm oid that is parsed from ASN.1 is unknown
     * @throws IOException              if the DER encoded key can't be parsed
     */
    private static String getKeyAlgorithmIdentifier(byte[] keyBytes) throws IOException, GeneralSecurityException {
        DerParser parser = new DerParser(keyBytes);
        DerParser.Asn1Object sequence = parser.readAsn1Object();
        parser = sequence.getParser();
        parser.readAsn1Object().getInteger(); // version
        DerParser.Asn1Object algSequence = parser.readAsn1Object();
        parser = algSequence.getParser();
        String oidString = parser.readAsn1Object().getOid();
        switch (oidString) {
            case "1.2.840.10040.4.1":
                return "DSA";
            case "1.2.840.113549.1.1.1":
                return "RSA";
            case "1.2.840.10045.2.1":
                return "EC";
        }
        throw new GeneralSecurityException(
                "Error parsing key algorithm identifier. Algorithm with OID: " + oidString + " is not " + "supported"
        );
    }

    private static String getAlgorithmNameFromOid(String oidString) throws GeneralSecurityException {
        return switch (oidString) {
            case "1.2.840.10040.4.1" -> "DSA";
            case "1.2.840.113549.1.1.1" -> "RSA";
            case "1.2.840.10045.2.1" -> "EC";
            case "1.3.14.3.2.7" -> "DES-CBC";
            case "2.16.840.1.101.3.4.1.1" -> "AES-128_ECB";
            case "2.16.840.1.101.3.4.1.2" -> "AES-128_CBC";
            case "2.16.840.1.101.3.4.1.3" -> "AES-128_OFB";
            case "2.16.840.1.101.3.4.1.4" -> "AES-128_CFB";
            case "2.16.840.1.101.3.4.1.6" -> "AES-128_GCM";
            case "2.16.840.1.101.3.4.1.21" -> "AES-192_ECB";
            case "2.16.840.1.101.3.4.1.22" -> "AES-192_CBC";
            case "2.16.840.1.101.3.4.1.23" -> "AES-192_OFB";
            case "2.16.840.1.101.3.4.1.24" -> "AES-192_CFB";
            case "2.16.840.1.101.3.4.1.26" -> "AES-192_GCM";
            case "2.16.840.1.101.3.4.1.41" -> "AES-256_ECB";
            case "2.16.840.1.101.3.4.1.42" -> "AES-256_CBC";
            case "2.16.840.1.101.3.4.1.43" -> "AES-256_OFB";
            case "2.16.840.1.101.3.4.1.44" -> "AES-256_CFB";
            case "2.16.840.1.101.3.4.1.46" -> "AES-256_GCM";
            case "2.16.840.1.101.3.4.1.5" -> "AESWrap-128";
            case "2.16.840.1.101.3.4.1.25" -> "AESWrap-192";
            case "2.16.840.1.101.3.4.1.45" -> "AESWrap-256";
            default -> null;
        };
    }

    private static String getEcCurveNameFromOid(String oidString) throws GeneralSecurityException {
        return switch (oidString) {
            // see https://tools.ietf.org/html/rfc5480#section-2.1.1.1
            case "1.2.840.10045.3.1" -> "secp192r1";
            case "1.3.132.0.1" -> "sect163k1";
            case "1.3.132.0.15" -> "sect163r2";
            case "1.3.132.0.33" -> "secp224r1";
            case "1.3.132.0.26" -> "sect233k1";
            case "1.3.132.0.27" -> "sect233r1";
            case "1.2.840.10045.3.1.7" -> "secp256r1";
            case "1.3.132.0.16" -> "sect283k1";
            case "1.3.132.0.17" -> "sect283r1";
            case "1.3.132.0.34" -> "secp384r1";
            case "1.3.132.0.36" -> "sect409k1";
            case "1.3.132.0.37" -> "sect409r1";
            case "1.3.132.0.35" -> "secp521r1";
            case "1.3.132.0.38" -> "sect571k1";
            case "1.3.132.0.39" -> "sect571r1";
            default -> throw new GeneralSecurityException(
                    "Error parsing EC named curve identifier. Named curve with OID: " + oidString + " is not " + "supported"
            );
        };

    }


    /**
     * Encodes the provided char[] to a UTF-8 byte[]. This is done while avoiding
     * conversions to String. The provided char[] is not modified by this method, so
     * the caller needs to take care of clearing the value if it is sensitive.
     * <p>
     * From <a href="https://github.com/elastic/elasticsearch/blob/a8cf4d6006c912af97c1407ff0406bc4a9a9659c/libs/core/src/main/java/org/elasticsearch/core/CharArrays.java#L20">...</a>
     *
     * @param chars The char array to encode to UTF-8.
     * @return The UTF-8 encoded byte array.
     */
    public static byte[] toUtf8Bytes(char[] chars) {
        final CharBuffer charBuffer = CharBuffer.wrap(chars);
        final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        final byte[] bytes;
        if (byteBuffer.hasArray()) {
            // there is no guarantee that the byte buffers backing array is the right size
            // so we need to make a copy
            bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
            Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        } else {
            final int length = byteBuffer.limit() - byteBuffer.position();
            bytes = new byte[length];
            byteBuffer.get(bytes);
            // if the buffer is not read only we can reset and fill with 0's
            if (byteBuffer.isReadOnly() == false) {
                byteBuffer.clear(); // reset
                for (int i = 0; i < byteBuffer.limit(); i++) {
                    byteBuffer.put((byte) 0);
                }
            }
        }
        return bytes;
    }

}
