package net.laprun.sustainability.power.sensors;

public record RegisteredPID(String stringForMatching) {

    public RegisteredPID(long pid) {
        this(prepare(pid));
    }

    public static String prepare(long pid) {
        // pad pid with spaces to have exact match on pid instead of randomly matching in the middle of something else
        return " " + pid + " ";
    }

    public static String prepare(String pidAsString) {
        return " " + pidAsString + " ";
    }

    public long pid() {
        return Long.parseLong(stringForMatching().trim());
    }

    public String pidAsString() {
        return stringForMatching().trim();
    }
}
