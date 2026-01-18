package com.stolink.backend.domain.work.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WorkFeedbackResponse {
    private long like;
    private long heart;
    private long coffee;
    private long next;
    private List<String> activeActions;
}
