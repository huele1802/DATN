package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.Hotel;
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

    @Query(value = "SELECT p.id, p.title, p.rating, p.address, p.review, p.slug, " +
            "ST_AsText(p.coordinates) AS coordinates_text, p.image_url, p.description, p.service, " +
            "ST_Distance(h.coordinates, p.coordinates) AS distance_in_meters " +
            "FROM hotels h, places p " +
            "WHERE h.id = :hotelId " +
            "AND ST_DWithin(h.coordinates, p.coordinates, :maxDistance) " +
            "ORDER BY distance_in_meters " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findNearbyPlaces(
            @Param("hotelId") Integer hotelId,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit);

    Optional<Place> findBySlug(String slug);
    /*
    @Query(value = "SELECT p.id, p.title, ST_Distance(h.coordinates, p.coordinates) AS distance " +
            "FROM hotels h, places p " +
            "WHERE h.id = :hotelId " +
            "AND ST_DWithin(h.coordinates, p.coordinates, :maxDistance) " +
            "ORDER BY distance " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findNearbyPlaces(@Param("hotelId") Integer hotelId,
                                    @Param("maxDistance") double maxDistance,
                                    @Param("limit") int limit);
     */
}
