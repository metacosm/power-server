package net.laprun.sustainability.power.analysis;

public class MeanComponentProcessor implements ComponentProcessor {
    private double mean;
    private int count;

    @Override
    public void recordComponentValue(double value, long timestamp) {
        final var previousSize = count;
        count++;
        mean = mean == 0 ? value : (previousSize * mean + value) / count;
    }
}
