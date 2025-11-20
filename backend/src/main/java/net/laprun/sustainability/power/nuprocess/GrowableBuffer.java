package net.laprun.sustainability.power.nuprocess;

import java.nio.ByteBuffer;

import io.quarkus.logging.Log;

class GrowableBuffer {
    private ByteBuffer buffer;

    public GrowableBuffer(int size) {
        buffer = ByteBuffer.allocate(size);
    }

    public void put(ByteBuffer input) {
        final var start = System.nanoTime();
        if (buffer.remaining() < input.remaining()) {
            final var sizeIncrease = input.remaining() - buffer.remaining();
            Log.debugf("Growing buffer. remaining: %d, asked: %d, capacity: %d, new capacity: %d", buffer.remaining(),
                    input.remaining(), buffer.capacity(), buffer.capacity() + sizeIncrease);
            var newBuffer = ByteBuffer.allocate(buffer.capacity() + sizeIncrease);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
        buffer.put(input);
        Log.debugf("putting buffer took %d ns", System.nanoTime() - start);
    }

    public byte[] array() {
        return buffer.array();
    }
}
