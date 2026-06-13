package com.lw.eco_expand.common.estorage.universal;

public final class UniversalStorageStats {

    private final int types;
    private final long storedCount;
    private final long usedBytes;

    public UniversalStorageStats(final int types, final long storedCount, final long usedBytes) {
        this.types = types;
        this.storedCount = storedCount;
        this.usedBytes = usedBytes;
    }

    public int types() {
        return types;
    }

    public long storedCount() {
        return storedCount;
    }

    public long usedBytes() {
        return usedBytes;
    }
}
