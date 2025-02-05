package net.laprun.sustainability.power.measure;

public record Cursor(int startIndex, int endIndex, double firstMeasureRatio, double lastMeasureRatio) {

    public static final Cursor empty = new Cursor(-1, -1, 0.0, 0.0);

    public double sum(double[] values) {
        if (values == null || values.length == 0 || this == empty || values.length <= endIndex) {
            return 0.0;
        }

        if (startIndex == endIndex) {
            return values[startIndex] * firstMeasureRatio;
        }

        double sum = values[startIndex] * firstMeasureRatio;
        for (int i = startIndex + 1; i < endIndex; i++) {
            sum += values[i];
        }
        sum += values[endIndex] * lastMeasureRatio;

        return sum;
    }

    public double[] viewOf(double[] values) {
        if (values == null || values.length == 0 || this == empty || values.length < startIndex + endIndex) {
            return new double[0];
        }

        if (startIndex == endIndex) {
            return new double[] { values[startIndex] * firstMeasureRatio };
        }

        final int len = endIndex - startIndex + 1;
        final double[] view = new double[len];
        view[0] = values[startIndex] * firstMeasureRatio;
        System.arraycopy(values, startIndex + 1, view, 1, len - 1 - 1);
        view[len - 1] = values[endIndex] * lastMeasureRatio;
        return view;
    }
}
