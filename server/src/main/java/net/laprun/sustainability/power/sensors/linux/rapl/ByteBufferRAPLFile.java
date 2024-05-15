package net.laprun.sustainability.power.sensors.linux.rapl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

class ByteBufferRAPLFile implements RAPLFile {
  private static final int CAPACITY = 64;
  private final ByteBuffer buffer;
  private final FileChannel channel;

  private ByteBufferRAPLFile(FileChannel channel) {
    this.channel = channel;
    buffer = ByteBuffer.allocate(CAPACITY);
  }

  static RAPLFile createFrom(Path file) {
    try {
      return new ByteBufferRAPLFile(new RandomAccessFile(file.toFile(), "r").getChannel());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public long extractEnergyInMicroJoules() {
    try {
      channel.read(buffer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    long value = 0;
    // will work even better if we can hard code as a static final const the length, in case won't change or is
    // defined by spec
    for (int i = 0; i < CAPACITY; i++) {
      byte digit = buffer.get(i);
      if (digit >= '0' && digit <= '9') {
        value = value * 10 + (digit - '0');
      } else {
        if (digit == '\n') {
          return value;
        }
        // Invalid character; handle accordingly or throw an exception
        throw new NumberFormatException("Invalid character in input: '" + Character.toString(digit) + "'");
      }
    }
    return value;
  }
}
