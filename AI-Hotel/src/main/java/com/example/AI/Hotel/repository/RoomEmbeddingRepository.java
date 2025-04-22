package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.RoomEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomEmbeddingRepository extends JpaRepository<RoomEmbedding, Integer> {
    long count();

    List<RoomEmbedding> findByRoomIdIn(List<Integer> roomIds);
    @Query(value = "SELECT r.room_id, r.text_embedding <=> CAST(:queryEmbedding AS vector) AS distance " +
            "FROM room_embeddings_backup r " +
            "WHERE he.text_embedding <=> CAST(:queryEmbedding AS vector) < :maxDistance " +
            "ORDER BY distance " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSimilarityRoom(@Param("queryEmbedding") String queryEmbedding,
                                        @Param("maxDistance") double maxDistance,
                                        @Param("limit") int limit);
//  boolean existsByRoomId(Integer roomId);
    @Query("SELECT re.roomId FROM RoomEmbedding re")
    List<Long> findAllRoomIds();

    @Query(value = "SELECT * FROM room_embeddings ORDER BY text_embedding <-> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<RoomEmbedding> findNearestRooms(@Param("embedding") String embedding, @Param("limit") int limit);
}
