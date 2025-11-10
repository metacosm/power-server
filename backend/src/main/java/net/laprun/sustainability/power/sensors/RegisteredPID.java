package net.laprun.sustainability.power.sensors;

public record RegisteredPID(String stringForMatching) {

    public static final long SYSTEM_TOTAL_PID = Long.MIN_VALUE;
    public static final RegisteredPID SYSTEM_TOTAL_REGISTERED_PID = new RegisteredPID(SYSTEM_TOTAL_PID);

    private RegisteredPID(long pid) {
        this(prepare(pid));
    }

    public static String prepare(long pid) {
        // pad pid with spaces to have exact match on pid instead of randomly matching in the middle of something else
        return " " + pid + " ";
    }

    public static String prepare(String pidAsString) {
        return " " + pidAsString + " ";
    }

    public static RegisteredPID create(long pid) {
        return SYSTEM_TOTAL_PID == pid ? SYSTEM_TOTAL_REGISTERED_PID : new RegisteredPID(pid);
    }

    public long pid() {
        return Long.parseLong(stringForMatching().trim());
    }

    public String pidAsString() {
        return stringForMatching().trim();
    }
}
