package com.stolink.backend.domain.work.entity;

public enum Genre {
    FANTASY,
    ROMANCE,
    MARTIAL_ARTS,
    MODERN_FANTASY,
    MYSTERY,
    THRILLER,
    SF,
    DRAMA,
    COMEDY,
    HORROR,
    OTHER;

    public static Genre from(String value) {
        if (value == null) return OTHER;
        try {
            return Genre.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
