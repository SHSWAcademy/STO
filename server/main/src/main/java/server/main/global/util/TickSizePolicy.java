package server.main.global.util;

import static server.main.global.error.ErrorCode.*;

import server.main.global.error.BusinessException;

public final class TickSizePolicy {

    private TickSizePolicy() {}

    public static long getTickSize(long price) {

        if (price < 100) {
            return 10;
        } else if (price < 1000) {
            return 50;
        } else if (price < 10000) {
            return 100;
        } else {
            return 500;
        }
    }

    public static void validate(long price) {
        long tickSize = getTickSize(price);
        if (price % tickSize != 0) {
            throw new BusinessException(INVALID_TICK_SIZE);
        }
    }
    
    
}
