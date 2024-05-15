package net.laprun.sustainability.power;

import io.quarkus.test.Mock;

@Mock
@SuppressWarnings("unused")
public class MockPowerMeasurer extends PowerMeasurer {

  @Override
  protected long validPIDOrFail(String pid) {
    return Long.parseLong(pid);
  }
}
