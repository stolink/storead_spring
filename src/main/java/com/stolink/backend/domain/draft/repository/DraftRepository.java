package com.stolink.backend.domain.draft.repository;

import com.stolink.backend.domain.draft.entity.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DraftRepository extends JpaRepository<Draft, UUID> {
}
