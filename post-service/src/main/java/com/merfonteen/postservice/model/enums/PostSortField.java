package com.merfonteen.postservice.model.enums;

import lombok.Getter;

@Getter
public enum PostSortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt");

    private final String fieldName;

    PostSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public static PostSortField from(String value) {
        for(PostSortField field : values()) {
            if(field.getFieldName().equalsIgnoreCase(value)) {
                return field;
            }
        }
        return CREATED_AT;
    }
}
