package inhibeans;

public interface Block extends AutoCloseable {

    static Block EMPTY_BLOCK = () -> {};

    /**
     * Releases this block. Does not throw.
     */
    @Override
    void close();
}
