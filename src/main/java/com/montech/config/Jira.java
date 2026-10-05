package com.montech.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.montech.dto.*;

public class Jira {

    private final String baseUrl;
    private final String authHeader;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Jira(String baseUrl, String email, String apiToken) {
        this.baseUrl = baseUrl;

        String auth = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        this.httpClient = HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(30))
              .build();

        this.objectMapper = new ObjectMapper();
    }

//   ADF (Atlassian Document Format)
    private Map<String, Object> toAdf(String text) {
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", text))
                ))
        );
    }

    private void validateDate(String date) {
        try {
            LocalDate.parse(date); //
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "duedate deve estar no formato yyyy-MM-dd, recebido: " + date, e);
        }
    }

    public String createIssue(String projectKey, String summary, String issueType,
                              String description, String name, String duedate)
          throws IOException, InterruptedException {
        // Monta o corpo da requisição usando os DTOs
        ProjectDto project = new ProjectDto(projectKey);
        IssueTypeDto type = new IssueTypeDto(issueType);
        IssueFieldsDto fields = new IssueFieldsDto(project, summary, type);

        if (description != null && !description.isBlank()) {
            fields.setDescription(toAdf(description));
        }
        if (name != null && !name.isBlank()) {
            fields.setPriority(new PriorityDto(name));
        }
        if (duedate != null && !duedate.isBlank()) {
            validateDate(duedate);
            fields.setDuedate(duedate);
        }

        IssueRequestDto issueRequest = new IssueRequestDto(fields);
        // Transforma o objeto Java em JSON
        String json = objectMapper.writeValueAsString(issueRequest);

        // Cria a requisição POST para o endpoint de criação de issues
        HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/rest/api/3/issue"))
              .timeout(Duration.ofSeconds(60))
              .header("Authorization", authHeader)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();

        // Envia a requisição
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return response.body();
        }

        throw new RuntimeException("Jira request failed: " + status + " - " + response.body());
    }
}
