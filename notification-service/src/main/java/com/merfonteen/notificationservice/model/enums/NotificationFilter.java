package com.merfonteen.notificationservice.model.enums;

public enum NotificationFilter {
    ALL("ALL"),
    READ("READ"),
    UNREAD("UNREAD");

    private final String fieldName;

    NotificationFilter(String fieldName) {
        this.fieldName = fieldName;
    }

    public static NotificationFilter from(String filter) {
        if(filter == null) {
            return ALL;
        }
        for(NotificationFilter notificationFilter : values()) {
            if(notificationFilter.fieldName.equalsIgnoreCase(filter)) {
                return notificationFilter;
            }
        }
        return ALL;
    }
}
