package com.jobseekercopilot.locationservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractAndDisabledStartupTest {
    @Value("${google.maps.enabled}")
    private boolean googleEnabled;

    @Value("${google.maps.gateway.service-token}")
    private String googleGatewayToken;

    @Test
    void startsWithGoogleDisabledAndNoGoogleCredential() {
        assertThat(googleEnabled).isFalse();
        assertThat(googleGatewayToken).isEqualTo("test-only-google-maps-gateway-token-32-bytes");
    }

    @Test
    void producerContractMatchesItsReviewedChecksum() throws Exception {
        byte[] contract = Files.readAllBytes(Path.of("api/openapi.yaml"));
        String expected = Files.readString(Path.of("api/SHA256SUMS")).split("\\s+")[0];
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contract)))
                .isEqualTo(expected);
        assertThat(new String(contract, java.nio.charset.StandardCharsets.UTF_8))
                .contains("version: 1.0.0");
    }

    @Test
    void googleGatewayClientPinsTheExactReviewedProducerRevision() throws Exception {
        byte[] contract = Files.readAllBytes(Path.of("src/main/openapi/google-maps-gateway.yaml"));
        String checksum = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(contract));
        String pin = Files.readString(Path.of("src/main/openapi/google-maps-gateway.pin.json"));

        assertThat(checksum)
                .isEqualTo("088c07471237d84aa0162c97f16f510ca3029e8fdd942e97156fe111b7ffc556");
        assertThat(pin)
                .contains("\"contractVersion\": \"1.0.0\"")
                .contains("\"sourceRevision\": \"1f57ea464dec6b72386b43f01bbb31ade3e577bd\"")
                .contains("\"sha256\": \"" + checksum + "\"")
                .contains("\"generatorVersion\": \"7.24.0\"");
    }
}
