package com.stolink.backend.support;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @org.springframework.beans.factory.annotation.Value("${spring.jpa.database-platform}")
    private String databasePlatform;

    @Transactional
    public void execute() {
        if (tableNames == null) {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
                    .map(this::getTableName)
                    .collect(Collectors.toList());
        }

        entityManager.flush();

        if (databasePlatform.contains("H2")) {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        } else if (databasePlatform.contains("PostgreSQL")) {
            entityManager.createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
        }

        for (String tableName : tableNames) {
            if (databasePlatform.contains("PostgreSQL")) {
                entityManager.createNativeQuery("TRUNCATE TABLE " + tableName + " CASCADE").executeUpdate();
            } else {
                entityManager.createNativeQuery("TRUNCATE TABLE " + tableName + " RESTART IDENTITY").executeUpdate();
            }
        }

        if (databasePlatform.contains("H2")) {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        } else if (databasePlatform.contains("PostgreSQL")) {
            entityManager.createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
        }
    }

    private String getTableName(EntityType<?> entity) {
        Table tableAnnotation = entity.getJavaType().getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        // 기본적으로 클래스명을 스네이크 케이스로 변환하는 등의 로직이 필요할 수 있으나,
        // 여기서는 단순함을 유지합니다. 실제 테이블명을 정확히 가져오기 위해 @Table 이름을 우선합니다.
        return entity.getName().toLowerCase(); // 단순화된 로직
    }
}
