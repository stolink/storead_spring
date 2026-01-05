package com.stolink.backend.domain.work.entity;

public enum Genre {
    FANTASY,
    ROMANCE,
    ROMANCE_FANTASY,
    TRADITIONAL_FANTASY,
    MARTIAL_ARTS,
    MODERN_FANTASY,
    MYSTERY,
    THRILLER,
    SF,
    DRAMA,
    COMEDY,
    HORROR,
    HEROIC_FANTASY,
    DARK_FANTASY,
    URBAN_FANTASY,
    HIGH_FANTASY,
    ISEKAI,
    OTHER;

    public static Genre from(String value) {
        if (value == null)
            return OTHER;
        try {
            return Genre.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
