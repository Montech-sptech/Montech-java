package com.montech.jira.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.montech.jira.dto.*;

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

    // ADF (Atlassian Document Format)
    private Map<String, Object> toAdf(String text) {
        List<Map<String, Object>> paragrafos = new ArrayList<>();
        for (String linha : text.split("\n")) {
            if (linha.isBlank()) continue;
            paragrafos.add(Map.of(
                    "type", "paragraph",
                    "content", List.of(Map.of("type", "text", "text", linha))));
        }
        return Map.of("type", "doc", "version", 1, "content", paragrafos);
    }

    private void validateDate(String date) {
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "duedate deve estar no formato yyyy-MM-dd, recebido: " + date, e);
        }
    }

    private String sanitizeLabel(String label) {
        if (label == null || !label.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException(
                    "Label inválida (use apenas letras, números, '-' e '_'): " + label);
        }
        return label;
    }

    public String createIssue(String projectKey, String summary, String issueType,
                              String description, String name, String duedate,
                              List<String> labels)
            throws IOException, InterruptedException {

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
        if (labels != null && !labels.isEmpty()) {
            labels.forEach(this::sanitizeLabel);
            fields.setLabels(labels);
        }

        IssueRequestDto issueRequest = new IssueRequestDto(fields);
        String json = objectMapper.writeValueAsString(issueRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/api/3/issue"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        }

        throw new RuntimeException("Jira request failed: " + status + " - " + response.body());
    }

    public boolean existeChamadoAberto(String projectKey, String labelServidor, String labelProblema)
            throws IOException, InterruptedException {

        if (projectKey == null || !projectKey.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Chave de projeto inválida: " + projectKey);
        }
        sanitizeLabel(labelServidor);
        sanitizeLabel(labelProblema);

        String jql = String.format(
                "project = %s AND statusCategory != Done AND labels = \"%s\" AND labels = \"%s\"",
                projectKey, labelServidor, labelProblema);

        String url = baseUrl + "/rest/api/3/search/jql?maxResults=1&fields=key&jql="
                + URLEncoder.encode(jql, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Falha ao consultar Jira: " + response.statusCode()
                    + " - " + response.body());
        }

        return !objectMapper.readTree(response.body()).path("issues").isEmpty();
    }
}