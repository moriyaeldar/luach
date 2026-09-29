package io.github.moriyaeldar.luach.calendar;

import java.util.Objects;

/** A piece of text in Hebrew and English, so clients can switch language without another request. */
public record LocalizedText(String he, String en) {
    public LocalizedText {
        Objects.requireNonNull(he, "he");
        Objects.requireNonNull(en, "en");
    }
}
