package net.laprun.sustainability.power;

import java.util.Objects;

import eu.hoefel.unit.Unit;

public class SensorUnit {

    private final Unit unit;
    private final String symbol;

    private SensorUnit(String symbol) {
        this.symbol = symbol;
        this.unit = Unit.of(symbol);
    }

    public static SensorUnit of(String unit) {
        return switch (unit) {
            case mW -> mWUnit;
            case W -> WUnit;
            case µJ -> µJUnit;
            case decimalPercentage -> decimalPercentageUnit;
            default -> new SensorUnit(unit);
        };
    }

    @SuppressWarnings("unused")
    public String getSymbol() {
        return symbol;
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SensorUnit that = (SensorUnit) o;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(symbol);
    }

    public static final String mW = "mW";
    public static final String W = "W";
    public static final String µJ = "µJ";
    public static final String decimalPercentage = "decimal percentage";

    private static final SensorUnit mWUnit = new SensorUnit(mW);
    private static final SensorUnit WUnit = new SensorUnit(W);
    private static final SensorUnit µJUnit = new SensorUnit(µJ);
    private static final SensorUnit decimalPercentageUnit = new SensorUnit(decimalPercentage);

    public boolean isWattCommensurable() {
        return equals(SensorUnit.WUnit) || unit.compatibleUnits().contains(SensorUnit.WUnit.unit);
    }
}
