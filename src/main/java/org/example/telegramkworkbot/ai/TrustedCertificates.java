package org.example.telegramkworkbot.ai;

import org.springframework.core.io.Resource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

/** Доверие к стандартным CA JVM плюс дополнительному сертификату (PEM или DER). */
public record TrustedCertificates(SSLSocketFactory socketFactory, X509TrustManager trustManager) {

    public static Optional<TrustedCertificates> withExtra(Resource cert) {
        if (cert == null || !cert.exists()) {
            return Optional.empty();
        }
        try (InputStream in = cert.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            int i = 0;
            for (X509Certificate ca : defaultTrustManager().getAcceptedIssuers()) {
                keyStore.setCertificateEntry("jvm-" + i++, ca);
            }
            for (Certificate extra : CertificateFactory.getInstance("X.509").generateCertificates(normalized(in))) {
                keyStore.setCertificateEntry("extra-" + i++, extra);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            X509TrustManager trustManager = x509(tmf.getTrustManagers());

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{trustManager}, null);
            return Optional.of(new TrustedCertificates(context.getSocketFactory(), trustManager));
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Не удалось загрузить сертификат " + cert, e);
        }
    }

    /**
     * Склеенные через cat PEM-файлы часто без перевода строки в конце:
     * "-----END CERTIFICATE----------BEGIN CERTIFICATE-----" — Java такое не читает.
     * DER (бинарный) файл возвращаем как есть.
     */
    private static InputStream normalized(InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        String text = new String(bytes, StandardCharsets.US_ASCII);
        if (!text.contains("-----BEGIN")) {
            return new ByteArrayInputStream(bytes);
        }
        String fixed = text.replace("-----END CERTIFICATE----------BEGIN", "-----END CERTIFICATE-----\n-----BEGIN") + "\n";
        return new ByteArrayInputStream(fixed.getBytes(StandardCharsets.US_ASCII));
    }

    private static X509TrustManager defaultTrustManager() throws GeneralSecurityException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        return x509(tmf.getTrustManagers());
    }

    private static X509TrustManager x509(TrustManager[] managers) {
        for (TrustManager tm : managers) {
            if (tm instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new IllegalStateException("X509TrustManager не найден");
    }
}
