package com.merfonteen.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class NotificationsSearchRequest {
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
    private String filterRaw;
}
