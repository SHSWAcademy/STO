package server.main.global.util;

import lombok.*;
import java.util.Collections;
import java.util.List;

public class GeminiDTO {

    // 하드코딩 상수 (시스템 설정용)
    public static final String MODEL_NAME = "gemini-2.5-flash";
    public static final double DEFAULT_TEMPERATURE = 0.2; // 금융 분석용 낮은 창의성
    public static final int DEFAULT_MAX_TOKENS = 1500;    // 요약용 길이 제한
    public static final double DEFAULT_TOP_P = 0.8;

    // 요청용
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private List<Content> contents;
        private GenerationConfig generationConfig;

        // 프롬프트만 넣어도 기본 설정값으로 객체를 생성해주는 편의 생성자
        public Request(String prompt) {
            this.contents = Collections.singletonList(new Content("user",
                    Collections.singletonList(new Part(prompt))));
            this.generationConfig = GenerationConfig.builder()
                    .temperature(DEFAULT_TEMPERATURE)
                    .maxOutputTokens(DEFAULT_MAX_TOKENS)
                    .topP(DEFAULT_TOP_P)
                    .build();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content {
        private String role; // 항상 "user"
        private List<Part> parts;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part {
        private String text; // 실제 프롬프트 내용
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GenerationConfig {
        private Double temperature;
        private Integer maxOutputTokens;
        private Double topP;
    }

    // 응답용
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Response {
        private List<Candidate> candidates;
        private UsageMetadata usageMetadata;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Candidate {
            private ContentResponse content;
            private String finishReason; // 정상 종료 여부 확인용 ("STOP")
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class ContentResponse {
            private List<Part> parts;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class UsageMetadata {
            private Integer totalTokenCount; // 무료 쿼터 모니터링용
        }

        // 결과 텍스트 도출
        public String getAnswer() {
            if (candidates == null || candidates.isEmpty()) return "분석 결과를 가져올 수 없습니다.";
            return candidates.get(0).getContent().getParts().get(0).getText();
        }
    }
}