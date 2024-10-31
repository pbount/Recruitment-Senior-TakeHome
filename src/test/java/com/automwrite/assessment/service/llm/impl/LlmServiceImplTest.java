package com.automwrite.assessment.service.llm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class LlmServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LlmServiceImpl llmService;

    private final String apiKey = "test-api-key";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        llmService = new LlmServiceImpl(restTemplate, objectMapper, apiKey);
    }

    @Test
    void testThatTheResponseContainsTheResponseTextWhenStatusIsOK() {
        /* Given */
        String prompt = "Request Prompt";
        String expectedResponseText = "Response Prompt";
        Map<String, Object> mockedResponse = Map.of("content", List.of(Map.of("text", expectedResponseText)));

        when(restTemplate.postForObject(eq("https://api.anthropic.com/v1/messages"),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockedResponse);

        /* When */
        String result = llmService.generateText(prompt);

        /* Then */
        assertEquals(expectedResponseText, result);
    }

    @Test
    void testThatResponseStringIsEmptyWhenResponseStatusIsUnauthorized() {
        /* Given */
        String prompt = "Request Prompt";

        when(restTemplate.postForObject(eq("https://api.anthropic.com/v1/messages"),
                any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("401 Unauthorized"));

        /* When */
        String result = llmService.generateText(prompt);

        /* Then */
        assertEquals("", result);
    }

    @Test
    void testThatResponseStringIsEmptyWhenResponseStatusIsInternalServerError() {
        /* Given */
        String prompt = "Request Prompt";


        when(restTemplate.postForObject(eq("https://api.anthropic.com/v1/messages"),
                any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("500 Internal Server Error"));

        /* When */
        String result = llmService.generateText(prompt);

        /* Then */
        assertEquals("", result);
    }
}