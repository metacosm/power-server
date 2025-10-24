package net.laprun.sustainability.power.nuprocess;

import java.nio.ByteBuffer;

class GrowableBuffer {
    private ByteBuffer buffer = ByteBuffer.allocate(20000);

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
