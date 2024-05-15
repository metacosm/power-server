package net.laprun.sustainability.power;

import java.util.Set;

import io.quarkus.test.junit.QuarkusTestProfile;

public class CIQuarkusTestProfile implements QuarkusTestProfile {

  @Override
  public Set<String> tags() {
    return Set.of("ci");
  }
}
