package com.stolink.backend.domain.draft.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Draft 엔티티 - stolink에서 생성한 drafts 테이블과 매핑
 * storead에서는 삭제 기능만 사용
 * 
 * @Immutable: 이 엔티티는 읽기/삭제 전용.
 * Hibernate가 UPDATE 쿼리를 생성하지 않으며, DDL 변경 위험을 최소화.
 */
@Entity
@Table(name = "drafts")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Draft {

    @Id
    private UUID id;
}
