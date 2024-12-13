package net.laprun.sustainability.power;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Errors {
    private List<String> errors;

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String formatErrors() {
        if (errors == null) {
            return "";
        }
        return errors.stream().collect(Collectors.joining("\n- ", "\n- ", ""));
    }
}
