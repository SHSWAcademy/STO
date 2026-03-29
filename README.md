# Convention

## 1. BaseEntity 상속받기

BaseEntity 추상 클래스 : 공용 필드 전용, createdAt, updatedAt을 가지고 있다.
다른 엔티티 클래스에서 extends BaseEntity 사용하면 createdAt, updatedAt을 매 번 쓸 필요 없다 (상속으로 공용 필드 전달)
그 외 컬럼 (executedAt 등 다른 날짜 필드가 필요할 경우 해당 엔티티 클래스에서 직접 추가 작성하기)

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class) 
public abstract class BaseEntity { 
    @CreatedDate
    @Column(updatable = false) 
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

=> `public class Token extends BaseEntity { ... }`

## 2. 엔티티 연관관계는 가급적 단방향

회원 탈퇴 시 주문 요청 삭제 등 Cascade로 반드시 묶어줄 경우가 아니면 단방향 관계로 설정하기

## 3. 모든 연관관계는 특별한 이유가 없다면 Lazy Loading으로 처리하기

## 4. 메서드 이름

컨트롤러 - 서비스 - 리포지토리 모두 통일 vs 각 계층별 메서드를 다르게 사용 => 뭐가 좋을까요

## 5. DTO

- DTO에서 validation 검증 하기 (@NotBlank, NotEmpty ..) => 엔티티 레벨에서 검증하지 말고 DTO 레벨에서 검증하고 엔티티로 전달하기
- DTO, Entity 에 setter 사용하지 말고 필요 시 별도 메서드를 만들기

```java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class ~Dto {
    @NotBlank
    private String hello;
    public void changeHello (String hello) {
      this.hello = hello;
    }
}
```

## 6. DTO <-> Entity 변환 시 MapStruct 사용하기 (실무 표준) & 컨트롤러는 dto만 주고 받고, 서비스 계층에서 DTO <-> Entity 변환

## 7. Entity 클래스 필드명

필드명은 name, id & @Column으로 user_name, token_id 처럼 DB 매핑

```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "token_id")
private Long id;

@Column(name = "user_name")
private String name;
```


## 8. RestController 리턴 타입 (실무 표준)

```java
@PostMapping("/hello")
public ResponseEntity<?> hello() { 
    return ResponseEntity.ok(testClass); 
}
```
리턴 타입으로 public TestObject hello() {..} 처럼 쓸 수도 있고 ResponseEntity<?> 로 쓸 수도 있다.

Swagger을 사용하면 제네릭으로 와일드카드 <?> 사용 시 정확하게 반영되지 않기 때문에 리턴 타입을 명시해서 쓰기

```java
@PostMapping("/hello")
public ResponseEntity<Void> hello() {
    return ResponseEntity.status(HttpStatus.OK).build();
}
```
위 코드처럼 리턴 타입을 <> 속에 명시하기


<예시>

return ResponseEntity.status(HttpStatus.OK).build() : 아무 데이터 없이 200 상태 코드만 반환

return ResponseEntity.ok(memberDto) : 200 상태 코드 + memberDto를 json으로 전달

return ResponseEntity.status(HttpStatus.CREATED).body(memberDto) : 201 Created 상태코드 + memberDto를 json으로 전달

return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/new-url").build()  
: 302 리다이렉트 상태코드 + 이동할 새로운 URL 주소를 헤더에 담아 전달


400에러, 500 에러 (실패 응답) : 전역 에러로 처리하기 (@RestControllerAdvice)

```java
// @RestControllerAdvice : 프로젝트 내 컨트롤러에서 발생하는 오류는 이 클래스에서 처리
@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<400에러가 떴을 때 리턴할 객체> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(400에러가 떴을 때 리턴할 객체); // 예 : ErrorResponse
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGeneralException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("서버 내부 오류가 발생했습니다.");
    }
}
```

```java
if (dto.getAge() < 0) {
    throw new IllegalArgumentException("나이는 0보다 작을 수 없습니다.");
}
```
위 처럼 컨트롤러에서 예외 발생 시 @ExceptionHandler가 붙은 메서드에서 전역 처리
