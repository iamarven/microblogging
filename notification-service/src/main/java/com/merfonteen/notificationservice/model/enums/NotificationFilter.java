package com.merfonteen.notificationservice.model.enums;

public enum NotificationFilter {
    ALL, READ, UNREAD;

    public static NotificationFilter from(String filter) {
        try {
            return NotificationFilter.valueOf(filter);
        } catch(IllegalArgumentException e) {
            return ALL;
        }
    }
}
