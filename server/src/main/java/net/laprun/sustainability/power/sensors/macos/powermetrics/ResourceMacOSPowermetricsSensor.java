package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

public class ResourceMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
  private final String resourceName;

  private boolean started;

  public ResourceMacOSPowermetricsSensor(String resourceName) {
    this.resourceName = resourceName;
    initMetadata(getInputStream());
  }

  @Override
  protected InputStream getInputStream() {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public void start(long samplingFrequencyInMillis) {
    if (!started) {
      started = true;
    }
  }
}
