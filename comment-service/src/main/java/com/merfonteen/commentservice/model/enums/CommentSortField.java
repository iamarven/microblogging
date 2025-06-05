package com.merfonteen.commentservice.model.enums;

import lombok.Getter;

@Getter
public enum CommentSortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt");

    private final String fieldName;

    CommentSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public static CommentSortField from(String sortBy) {
        for(CommentSortField field : values()) {
            if(field.getFieldName().equalsIgnoreCase(sortBy)) {
                return field;
            }
        }
        return CREATED_AT;
    }
}
