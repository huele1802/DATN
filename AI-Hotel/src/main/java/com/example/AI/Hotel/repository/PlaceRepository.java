package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlaceRepository extends JpaRepository<Place, Integer> {
    Page<Place> findAll(Pageable pageable);
//    // Tìm địa điểm theo tên (gần đúng)
//    @Query("SELECT p FROM Place p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))")
//    Optional<Place> findByTitleIgnoreCase(@Param("title") String title);
//
//    // Tìm địa điểm theo nhiều tiêu chí
//    @Query(value = "SELECT p.* FROM places p " +
//            "WHERE (:title IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
//            "AND (:rating IS NULL OR LOWER(p.rating) LIKE LOWER(CONCAT('%', :rating, '%'))) " +
//            "AND (:reviewCount IS NULL OR p.review_count = :reviewCount) " +
//            "AND (:phoneNumber IS NULL OR LOWER(p.phone_number) LIKE LOWER(CONCAT('%', :phoneNumber, '%'))) " +
//            "AND (:address IS NULL OR LOWER(p.address) LIKE LOWER(CONCAT('%', :address, '%'))) " +
//            "AND (:latitude IS NULL OR :longitude IS NULL OR ST_DWithin(p.coordinates, ST_GeomFromText('POINT(:longitude :latitude)', 4326), :radius))",
//            nativeQuery = true)
//    List<Place> findByCriteria(
//            @Param("title") String title,
//            @Param("rating") String rating,
//            @Param("reviewCount") String reviewCount,
//            @Param("phoneNumber") String phoneNumber,
//            @Param("address") String address,
//            @Param("latitude") Double latitude,
//            @Param("longitude") Double longitude,
//            @Param("radius") Double radius
//    );
}