package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.HotelTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HotelTripRepository extends JpaRepository<HotelTrip, Integer> {

    @Query("SELECT ht FROM HotelTrip ht WHERE ht.id = :id AND ht.userId = :userId")
    Optional<HotelTrip> findByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    @Query("SELECT ht FROM HotelTrip ht WHERE ht.userId = :userId AND ht.hotelId = :hotelId")
    Optional<HotelTrip> findByUserIdAndHotelId(@Param("userId") Integer userId, @Param("hotelId") Integer hotelId);
}
