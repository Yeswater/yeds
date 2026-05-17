package com.yeswater.iam.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AuthorizeBatchCheckRequest(
        @NotEmpty(message = "items不能为空") List<@Valid AuthorizeCheckRequest> items
) {
}
