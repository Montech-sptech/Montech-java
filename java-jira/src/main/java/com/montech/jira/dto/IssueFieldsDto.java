package com.montech.jira.dto;

import java.util.ArrayList;
import java.util.List;

public class IssueFieldsDto {

    private ProjectDto project;
    private String summary;

    private IssueTypeDto issuetype;

    private Object description;
    private PriorityDto priority;
    private String duedate;

    private List<String> labels;

    public IssueFieldsDto(ProjectDto project, String summary, IssueTypeDto issuetype) {
        this.project = project;
        this.summary = summary;
        this.issuetype = issuetype;
        this.labels = new ArrayList<>();
    }

    public ProjectDto getProject() {
        return project;
    }

    public void setProject(ProjectDto project) {
        this.project = project;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public IssueTypeDto getIssuetype() {
        return issuetype;
    }

    public void setIssuetype(IssueTypeDto issuetype) {
        this.issuetype = issuetype;
    }

    public Object getDescription() {
        return description;
    }

    public void setDescription(Object description) {
        this.description = description;
    }

    public PriorityDto getPriority() {
        return priority;
    }

    public void setPriority(PriorityDto priority) {
        this.priority = priority;
    }

    public String getDuedate() {
        return duedate;
    }

    public void setDuedate(String duedate) {
        this.duedate = duedate;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
}
