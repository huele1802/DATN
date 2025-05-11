package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {
    Page<Hotel> findAll(Pageable pageable);

    // Tìm khách sạn theo danh sách ID và district
    @Query("SELECT h FROM Hotel h WHERE h.id IN :ids AND h.district = :district")
    List<Hotel> findAllByIdAndDistrict(@Param("ids") Iterable<Integer> ids, @Param("district") String district);

    @Query(value = "SELECT h.id, h.embedding, 1 - (h.embedding <=> CAST(:embedding AS vector)) AS similarity " +
            "FROM hotels h " +
            "WHERE (1 - (h.embedding <=> CAST(:embedding AS vector))) > :threshold " +
            "ORDER BY similarity DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSimilarHotelsForSearchV2(
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("limit") int limit);

    @Query(value = """
        SELECT 
            he.hotel_id,
            h.name AS hotel_name,
            (1 - (he.embedding <=> CAST(:queryEmbedding AS vector))) AS similarity
        FROM hotel_embeddings he
        JOIN hotels h ON he.hotel_id = h.id
        WHERE (1 - (he.embedding <=> CAST(:queryEmbedding AS vector))) >= :threshold
        ORDER BY similarity DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopSimilarHotels(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("threshold") double threshold,
            @Param("limit") int limit
    );

    Optional<Hotel> findBySlug(String slug);
}