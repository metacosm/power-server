package net.laprun.sustainability.power.nuprocess;

import java.nio.ByteBuffer;

class GrowableBuffer {
    private static final int DEFAULT_SIZE = 20000;
    private ByteBuffer buffer;

    public GrowableBuffer() {
        this(DEFAULT_SIZE);
    }

    public GrowableBuffer(int size) {
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        buffer = ByteBuffer.allocate(size);
    }

    public void put(ByteBuffer input) {
        if (buffer.remaining() < input.remaining()) {
            var newBuffer = ByteBuffer.allocate(buffer.capacity() + input.remaining());
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        } else {
            buffer.put(input);
        }
    }

    public byte[] array() {
        return buffer.array();
    }
}
