package com.stolink.backend.domain.draft.repository;

import com.stolink.backend.domain.draft.entity.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface DraftRepository extends JpaRepository<Draft, UUID> {

    /**
     * 단일 DELETE 쿼리로 Draft 삭제
     * @return 삭제된 행 수 (0이면 해당 ID 없음)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Draft d WHERE d.id = :id")
    int deleteByIdAndReturnCount(@Param("id") UUID id);
}
