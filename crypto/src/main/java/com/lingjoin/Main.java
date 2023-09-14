package com.lingjoin;

import com.lingjoin.cert.CertUtils;
import com.lingjoin.cert.PemUtils;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.OperatorCreationException;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class Main {
    public static void main(String[] args) throws IOException {
        byte[] bytes = Main.generateSecretKey();
        FileUtils.writeByteArrayToFile(Paths.get("output", "out.zip").toFile(), bytes);
    }

    public static byte[] generateSecretKey() {
        String test = """
                    7 * 11
                """;
        Path certs = Paths.get("C:", "ProgramData", "openssl", "certs");
        Path caPem = certs.resolve("ca.pem");
        Path privateKey = certs.resolve("server.key");

        // 生成密钥对
        KeyPair keyPair = CertUtils.generateRSAPair();

        // 生成签名
        try (
                InputStream inputStream = Files.newInputStream(caPem);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            // 读取私钥
            PrivateKey caPrivateKey = PemUtils.readPrivateKey(privateKey, null);
            // 读取CA签名
            X509Certificate caCert = CertUtils.readX509Certificate(inputStream);
            // 生成签名证书
            X509Certificate x509Certificate = CertUtils.generateSignedCertificate(new X500Principal(String.join(", ",
                            new String[]{
                                    "C= CN",
                                    "ST= Beijing",
                                    "L= Beijing",
                                    "O= LingJoin Co. Ltd.",
                                    "CN = Scanner Server"
                            }
                    )), new GeneralNames(new GeneralName[]{
                            new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
                    }),
                    null,
                    caPrivateKey,
                    caCert,
                    keyPair,
                    365 * 10
            );
            // 存储server密钥和证书

            CertUtils.saveCertAndKey(byteArrayOutputStream, x509Certificate, keyPair.getPrivate(), null, caPem.toString());
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
