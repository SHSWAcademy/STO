package server.main.global.util;

import server.main.global.error.BusinessException;
import static server.main.global.error.ErrorCode.INVALID_TICK_SIZE;

public class TickSizePolicy {

    public static long getTicksize(long price) {

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
        long tickSize = getTicksize(price);
        if (price % tickSize != 0) {
            throw new BusinessException(INVALID_TICK_SIZE);
        }
    }
    
    
}
