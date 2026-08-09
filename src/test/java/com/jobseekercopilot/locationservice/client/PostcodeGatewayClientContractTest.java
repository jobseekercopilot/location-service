package com.jobseekercopilot.locationservice.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
}
