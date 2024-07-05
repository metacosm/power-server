package net.laprun.sustainability.power;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.hoefel.unit.Unit;

public class SensorUnit {

    private final Unit unit;
    private final String symbol;

    @JsonCreator
    public SensorUnit(String symbol) {
        this.symbol = symbol;
        this.unit = Unit.of(symbol);
    }

    public String getSymbol() {
        return symbol;
    }

    @JsonIgnore
    public Unit getUnit() {
        return unit;
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

    public static final SensorUnit mW = new SensorUnit("mW");
    public static final SensorUnit W = new SensorUnit("W");
    public static final SensorUnit µJ = new SensorUnit("µJ");
    public static final SensorUnit decimalPercentage = new SensorUnit("decimal percentage");
}
