package io.github.moriyaeldar.luach.calendar;

/**
 * Hebrew months as a user would pick them in a form.
 *
 * <p>{@link #ADAR} is "Adar" of a regular year. {@link #ADAR_I} and {@link #ADAR_II} exist only in leap
 * years; a date anchored to one of them falls back to plain Adar in a regular year. Which Adar a plain
 * {@link #ADAR} date lands on in a leap year is decided by a {@link LeapYearPolicy}.
 */
public enum HebrewMonth {
    TISHREI(7, "תשרי", "Tishrei"),
    CHESHVAN(8, "חשוון", "Cheshvan"),
    KISLEV(9, "כסלו", "Kislev"),
    TEVET(10, "טבת", "Tevet"),
    SHEVAT(11, "שבט", "Shevat"),
    ADAR(12, "אדר", "Adar"),
    ADAR_I(12, "אדר א׳", "Adar I"),
    ADAR_II(13, "אדר ב׳", "Adar II"),
    NISAN(1, "ניסן", "Nisan"),
    IYAR(2, "אייר", "Iyar"),
    SIVAN(3, "סיוון", "Sivan"),
    TAMMUZ(4, "תמוז", "Tammuz"),
    AV(5, "אב", "Av"),
    ELUL(6, "אלול", "Elul");

    /** Month number as used by KosherJava (Nisan = 1 ... Adar = 12, Adar II = 13). */
    private final int kosherJavaNumber;
    private final LocalizedText name;

    HebrewMonth(int kosherJavaNumber, String he, String en) {
        this.kosherJavaNumber = kosherJavaNumber;
        this.name = new LocalizedText(he, en);
    }

    public LocalizedText displayName() {
        return name;
    }

    int kosherJavaNumber() {
        return kosherJavaNumber;
    }

    /** Maps a KosherJava month number back to the month a user would recognise in that year. */
    static HebrewMonth fromKosherJava(int month, boolean leapYear) {
        if (month == 12) {
            return leapYear ? ADAR_I : ADAR;
        }
        if (month == 13) {
            return ADAR_II;
        }
        for (HebrewMonth m : values()) {
            if (m.kosherJavaNumber == month) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown Hebrew month number: " + month);
    }
}
