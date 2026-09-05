package com.backend.protection.controller;

import com.backend.protection.dto.auth.LoginRequest;
import com.backend.protection.dto.auth.OtpRequest;
import com.backend.protection.repository.PatientRepository;
import com.backend.protection.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class E2EFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testDoctorLoginOtpAndAccessEncryptedRecords() throws Exception {
        // Step 1: Doctor Login initiates OTP
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("doctor@ecare.com");
        loginReq.setPassword("Doctor@1234");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String loginJson = loginResult.getResponse().getContentAsString();
        JsonNode loginNode = objectMapper.readTree(loginJson);
        assertTrue(loginNode.get("success").asBoolean());

        // Extract tempOtp provided in response for verification
        String tempOtp = loginNode.get("data").get("tempOtp").asText();
        assertNotNull(tempOtp);
        assertEquals(6, tempOtp.length());

        // Step 2: Verify OTP and acquire JWT
        OtpRequest otpReq = new OtpRequest();
        otpReq.setEmail("doctor@ecare.com");
        otpReq.setOtp(tempOtp);
        otpReq.setPurpose("LOGIN");

        MvcResult otpResult = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpReq)))
                .andExpect(status().isOk())
                .andReturn();

        String otpJson = otpResult.getResponse().getContentAsString();
        JsonNode otpNode = objectMapper.readTree(otpJson);
        assertTrue(otpNode.get("success").asBoolean());
        String token = otpNode.get("data").get("accessToken").asText();
        assertNotNull(token);
        assertEquals("DOCTOR", otpNode.get("data").get("user").get("role").asText());

        // Step 3: Access Patients list using JWT
        MvcResult patientResult = mockMvc.perform(get("/api/patients")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode patientNode = objectMapper.readTree(patientResult.getResponse().getContentAsString());
        assertTrue(patientNode.get("success").asBoolean());
        assertTrue(patientNode.get("data").get("content").size() > 0);

        String firstPatientId = patientNode.get("data").get("content").get(0).get("id").asText();

        // Step 4: Access and Decrypt Medical Records for patient
        MvcResult recordsResult = mockMvc.perform(get("/api/medical-records/patient/" + firstPatientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode recordsNode = objectMapper.readTree(recordsResult.getResponse().getContentAsString());
        assertTrue(recordsNode.get("success").asBoolean());
        // Verify decrypted diagnosis is visible to Doctor
        if (recordsNode.get("data").size() > 0) {
            String diagnosis = recordsNode.get("data").get(0).get("diagnosis").asText();
            assertNotNull(diagnosis);
            assertFalse(diagnosis.isEmpty());
            assertTrue(recordsNode.get("data").get(0).get("encrypted").asBoolean());
        }
    }
}
