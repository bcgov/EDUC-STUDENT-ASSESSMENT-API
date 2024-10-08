package ca.bc.gov.educ.eas.api.util;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Utility class for handling datatype UUID.
 */
public final class UUIDUtil {
    private UUIDUtil() {
    }

    public static UUID asUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static UUID fromString(String uuid) {
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }
}
