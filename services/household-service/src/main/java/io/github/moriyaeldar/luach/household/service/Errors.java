package io.github.moriyaeldar.luach.household.service;

/** Domain errors, mapped to HTTP problem details by the web layer. */
public final class Errors {

    private Errors() {
    }

    public static class NotFound extends RuntimeException {
        public NotFound(String message) {
            super(message);
        }
    }

    public static class Forbidden extends RuntimeException {
        public Forbidden(String message) {
            super(message);
        }
    }

    public static class Invalid extends RuntimeException {
        public Invalid(String message) {
            super(message);
        }
    }

    /** The invite exists but was already used or has expired. */
    public static class Gone extends RuntimeException {
        public Gone(String message) {
            super(message);
        }
    }
}
