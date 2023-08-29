package io.github.yangyaofei.crypto;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * parse and generate RSA Cert
 */
@SuppressWarnings("unused")
public class CertUtils {
    private static final BouncyCastleProvider BC_PROV = new BouncyCastleProvider();
    private static final int SERIAL_BIT_LENGTH = 20 * 8;


    /**
     * Generate RAS key pair.
     *
     * @return the key pair
     */
    public static KeyPair generateRSAPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 生成签名证书
     *
     * @param principal          证书主体
     * @param subjectAltNames    扩展主体名称
     * @param signatureAlgorithm 签名算法类型
     * @param caPrivateKey       ca私钥
     * @param caCert             ca证书
     * @param keyPair            服务端证书
     * @param days               有效日期
     * @return 签名证书 x 509 certificate
     * @throws NoSuchAlgorithmException  the no such algorithm exception
     * @throws OperatorCreationException the operator creation exception
     * @throws CertificateException      the certificate exception
     * @throws CertIOException           the cert io exception
     */
    public static X509Certificate generateSignedCertificate(
            final X500Principal principal,
            final GeneralNames subjectAltNames,
            @Nullable final String signatureAlgorithm,
            @Nullable final PrivateKey caPrivateKey,
            @Nullable final X509Certificate caCert,
            final KeyPair keyPair,
            final int days
    ) throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, CertIOException {
        //生成签名随机序列
        final BigInteger serial = CertUtils.getSerial();
        //读取配置文件的主体信息
        final X500Name subject = X500Name.getInstance(principal.getEncoded());
        // 证书颁发者
        final X500Name issuer;
        final AuthorityKeyIdentifier authorityKeyIdentifier;

        //证书的起始有效时间和证书过期时间
        final Instant notBefore = Instant.now();
        final Instant notAfter = notBefore.plus(days, ChronoUnit.DAYS);

        // 生成签名文件使用的私钥, 如果ca为null则使用自身私钥进行自签名
        final PrivateKey signingKey = caPrivateKey != null ? caPrivateKey : keyPair.getPrivate();

        // SHA-1摘要计算器
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        // 如果ca为null则进行自签名
        if (caCert != null) {
            if (caCert.getBasicConstraints() < 0) {
                throw new IllegalArgumentException("ca certificate is not a CA!");
            }
            issuer = X500Name.getInstance(caCert.getIssuerX500Principal().getEncoded());
            authorityKeyIdentifier = extUtils.createAuthorityKeyIdentifier(caCert.getPublicKey());
        } else {
            issuer = subject;
            authorityKeyIdentifier = extUtils.createAuthorityKeyIdentifier(keyPair.getPublic());
        }

        // 构建对象
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                Date.from(notBefore),
                Date.from(notAfter),
                subject,
                keyPair.getPublic()
        );


        //创建签名操作员
        ContentSigner signer = new JcaContentSignerBuilder(
                signatureAlgorithm == null ? CertUtils.getDefaultSignatureAlgorithm(caPrivateKey) : signatureAlgorithm
        )
                .setProvider(CertUtils.BC_PROV)
                .build(signingKey);
        builder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
        if (subjectAltNames != null) {
            builder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        }
        builder.addExtension(Extension.basicConstraints, caCert == null, new BasicConstraints(caCert == null));


        X509CertificateHolder certificateHolder = builder.build(signer);
        // 生成X509证书
        JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
        return jcaX509CertificateConverter.getCertificate(certificateHolder);
    }

    /**
     * 生成证书的随机序列
     *
     * @return the serial
     */
    public static BigInteger getSerial() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(CertUtils.SERIAL_BIT_LENGTH, random);
    }

    private static String getDefaultSignatureAlgorithm(PrivateKey key) {
        return switch (key.getAlgorithm()) {
            case "RSA" -> "SHA256withRSA";
            case "DSA" -> "SHA256withDSA";
            case "EC" -> "SHA256withECDSA";
            default -> throw new IllegalArgumentException(
                    "Unsupported algorithm : "
                            + key.getAlgorithm()
                            + " for signature, allowed values for private key algorithm are [RSA, DSA, EC]"
            );
        };
    }

    /**
     * Saves the certificate and private key to an output stream in a zip file format.
     *
     * @param  outputStream   the output stream to write the zip file to
     * @param  certificate     the X509 certificate to save
     * @param  privateKey      the private key to save
     * @param  password        the password to encrypt the private key (optional)
     * @param  caPath         the path to the CA certificate file
     */
    public static void saveCertAndKey(
            OutputStream outputStream,
            X509Certificate certificate,
            PrivateKey privateKey,
            @Nullable String password,
            String caPath
    ) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
             JcaPEMWriter pemWriter = new JcaPEMWriter(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8));
             FileInputStream caInputStream = new FileInputStream(caPath)
        ) {
            zipOutputStream.putNextEntry(new ZipEntry("client.crt"));
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            zipOutputStream.closeEntry();
            PemObject pemObject = new PemObject("PRIVATE KEY", privateKey.getEncoded());

            zipOutputStream.putNextEntry(new ZipEntry("client.key"));
            if (password != null) {
                pemWriter.writeObject(
                        pemObject,
                        new JcePEMEncryptorBuilder("AES-128-CBC")
                                .setProvider(CertUtils.BC_PROV)
                                .build(password.toCharArray())
                );
            } else {
                pemWriter.writeObject(pemObject);
            }
            pemWriter.flush();
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry("ca.pem"));
            IOUtils.copy(caInputStream, zipOutputStream);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read x 509 certificate x 509 certificate.
     *
     * @param input the input
     * @return the x 509 certificate
     * @throws Exception the exception
     */
    public static X509Certificate readX509Certificate(InputStream input) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(input);
    }
}
