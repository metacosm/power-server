package net.laprun.sustainability.power.measure;

public enum Compute {
    ;

    public static double sumOfComponents(double[] recorded) {
        var componentSum = 0.0;
        for (double value : recorded) {
            componentSum += value;
        }
        return componentSum;
    }

    public static double sumOfSelectedComponents(double[] recorded, int... indices) {
        if (indices == null || indices.length == 0) {
            return sumOfComponents(recorded);
        }
        var componentSum = 0.0;
        for (int index : indices) {
            componentSum += recorded[index];
        }
        return componentSum;
    }
}
