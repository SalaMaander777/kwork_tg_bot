package org.example.telegramkworkbot.ai;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedCertificatesTest {

    private static final ClassPathResource BUNDLE = new ClassPathResource("certs/russian_trusted_ca.pem");

    @Test
    void loadsBundledCertificates() {
        assertThat(BUNDLE.exists()).isTrue();
        assertThat(TrustedCertificates.withExtra(BUNDLE)).isPresent();
    }

    @Test
    void acceptsPemFilesGluedWithoutNewline() throws Exception {
        String pem = BUNDLE.getContentAsString(StandardCharsets.US_ASCII)
                .replace("-----END CERTIFICATE-----\n-----BEGIN", "-----END CERTIFICATE----------BEGIN")
                .stripTrailing();
        assertThat(pem).contains("----------BEGIN");

        var tls = TrustedCertificates.withExtra(new ByteArrayResource(pem.getBytes(StandardCharsets.US_ASCII)));

        assertThat(tls).isPresent();
        assertThat(tls.get().trustManager().getAcceptedIssuers())
                .anyMatch(ca -> ca.getSubjectX500Principal().getName().contains("Russian Trusted"));
    }

    @Test
    void missingFileMeansDefaultTrust() {
        assertThat(TrustedCertificates.withExtra(new ClassPathResource("certs/nope.pem"))).isEmpty();
    }
}
