package server.main.global.error.exception;

import lombok.Getter;
import sto.general.global.error.ErrorCode;

@Getter
public class BusinessException extends RuntimeException {
    private ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
