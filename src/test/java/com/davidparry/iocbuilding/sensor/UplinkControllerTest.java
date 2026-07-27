package com.davidparry.iocbuilding.sensor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UplinkController.class)
@Import(LoraDecodingService.class)
class UplinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ingestsUplinkAndReturnsDecodedReadings() throws Exception {
        mockMvc.perform(post("/api/v1/uplink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"devEui":"70B3D5CAFE000001","payloadHex":"036700FA0900FF"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devEui").value("70B3D5CAFE000001"))
                .andExpect(jsonPath("$.readings.length()").value(2))
                .andExpect(jsonPath("$.readings[0].type").value("T"))
                .andExpect(jsonPath("$.readings[0].value").value(25.0))
                .andExpect(jsonPath("$.readings[1].type").value("MD"));
    }

    @Test
    void malformedHexIsRejectedAndLogged() throws Exception {
        mockMvc.perform(post("/api/v1/uplink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"devEui":"70B3D5CAFE000001","payloadHex":"zznothex"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingPayloadIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/uplink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"devEui":"70B3D5CAFE000001"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
