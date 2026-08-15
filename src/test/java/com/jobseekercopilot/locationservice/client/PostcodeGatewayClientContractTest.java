package com.jobseekercopilot.locationservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class PostcodeGatewayClientContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsThePostcodeGatewaySnakeCaseDistrictField() throws Exception {
        String response = """
                {
                  "postcode": "RG1 1AA",
                  "country": "England",
                  "region": "South East",
                  "admin_district": "Reading",
                  "latitude": 51.4543,
                  "longitude": -0.9781
                }
                """;

        PostcodeGatewayClient.PostcodeResult result = objectMapper.readValue(
                response, PostcodeGatewayClient.PostcodeResult.class);

        assertThat(result.adminDistrict()).isEqualTo("Reading");
    }

    @Test
    void preservesUnsupportedCoverageWithoutReadingOrLeakingProviderBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PostcodeGatewayClient client = new PostcodeGatewayClient(builder, "http://postcode.test");
        server.expect(once(), requestTo("http://postcode.test/api/postcodes/BT11AA"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("private provider detail for BT11AA"));

        assertThatThrownBy(() -> client.lookup("BT11AA"))
                .isInstanceOfSatisfying(ResponseStatusException.class, failure -> {
                    assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(failure.getReason())
                            .isEqualTo("This postcode area is not currently supported.")
                            .doesNotContain("BT11AA", "private");
                });
        server.verify();
    }

    @Test
    void mapsUnexpectedProviderFailureToRedactedBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PostcodeGatewayClient client = new PostcodeGatewayClient(builder, "http://postcode.test");
        server.expect(once(), requestTo("http://postcode.test/api/postcodes/LS1"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("private provider failure"));

        assertThatThrownBy(() -> client.lookup("LS1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, failure -> {
                    assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(failure.getReason())
                            .isEqualTo("Location provider returned an invalid response.")
                            .doesNotContain("private", "LS1");
                });
        server.verify();
    }
}
