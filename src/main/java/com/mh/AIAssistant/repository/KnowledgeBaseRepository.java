package com.mh.AIAssistant.repository;

import com.mh.AIAssistant.model.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeEntry, Long> {
    
    List<KnowledgeEntry> findByUserId(String userId);
    
    /**
     * Find similar entries using cosine similarity with PostgreSQL arrays
     * Returns entries sorted by similarity (highest first)
     */
    @Query(value = """
        WITH query_vec AS (
            SELECT CAST(:embedding AS double precision[]) as vec
        ),
        similarities AS (
            SELECT 
                k.*,
                (
                    SELECT SUM(a * b) / (
                        SQRT(SUM(a * a)) * SQRT(SUM(b * b))
                    )
                    FROM (
                        SELECT 
                            unnest(k.embedding) as a,
                            unnest(q.vec) as b
                        FROM query_vec q
                    ) dot_product
                ) as similarity_score
            FROM knowledge_entries k
            CROSS JOIN query_vec q
            WHERE k.user_id = :userId
        )
        SELECT id, user_id, content, embedding, created_at, similarity_score,
            file_path, file_name, file_type
        FROM similarities
        WHERE similarity_score IS NOT NULL
        ORDER BY similarity_score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarEntriesRaw(
        @Param("embedding") String embedding, 
        @Param("userId") String userId, 
        @Param("limit") int limit
    );

}