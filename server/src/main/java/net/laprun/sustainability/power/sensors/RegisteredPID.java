package net.laprun.sustainability.power.sensors;

public record RegisteredPID(String stringForMatching) {

    public RegisteredPID(long pid) {
        this(prepare(pid));
    }

    public static String prepare(long pid) {
        return " " + pid + " ";
    }

    public long pid() {
        return Long.parseLong(stringForMatching().trim());
    }
}
