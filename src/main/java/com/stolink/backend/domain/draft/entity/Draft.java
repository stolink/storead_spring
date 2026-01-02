package com.stolink.backend.domain.draft.entity;

import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Draft 엔티티 - stolink에서 생성한 drafts 테이블과 매핑
 * storead에서는 삭제 기능만 사용
 */
@Entity
@Table(name = "drafts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Draft {

    @Id
    private UUID id;

    // 삭제 기능만 사용하므로 다른 필드는 최소화
    // stolink에서 정의한 테이블 구조에 맞춰 필요 시 확장
}
