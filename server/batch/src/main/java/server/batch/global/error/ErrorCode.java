package server.batch.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    ;
    // 도연님 에러코드 확인해보시고 유동적으로 추가나 삭제 부탁드립니다 ! 아래는 예시라서

    private final String errorCode;
    private final String message;
}
