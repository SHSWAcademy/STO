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

컨트롤러 - 서비스 - 리포지토리 모두 통일 vs 각 계층별 메서드를 다르게 사용 => 정하기

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

## 6. DTO <-> Entity 변환 시 MapStruct 사용하기 (실무 표준)

## 7. Entity 클래스 필드명

필드명은 name, id & @Column으로 user_name, token_id 처럼 DB 매핑

```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "token_id")
private Long id;

@Column(name = "user_name")
private String name;
```
