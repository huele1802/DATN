package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {
    Page<Hotel> findAll(Pageable pageable);
//    @Query(value = """
//        SELECT *
//        FROM hotels h
//        WHERE (:facilityValue IS NULL OR h.facilities @> ARRAY[:facilityValue]::jsonb)
//        AND (:highlightValue IS NULL OR EXISTS (
//            SELECT 1
//            FROM jsonb_each_text(h.highlights) hl
//            WHERE hl.value ILIKE '%' || :highlightValue || '%'
//        ))
//        AND (:minRatingStars IS NULL OR h.rating_stars >= :minRatingStars)
//        AND (
//            :latitude IS NULL
//            OR :longitude IS NULL
//            OR ST_DWithin(
//                ST_GeomFromText(h.coordinates),
//                ST_MakePoint(:longitude, :latitude),
//                :radius
//            )
//        )
//        """, nativeQuery = true)
//    List<Hotel> findHotelsByCriteria(
//            @Param("facilityValue") String facilityValue,
//            @Param("highlightValue") String highlightValue,
//            @Param("minRatingStars") Integer minRatingStars,
//            @Param("latitude") Double latitude,
//            @Param("longitude") Double longitude,
//            @Param("radius") Double radius
//
//    );
}