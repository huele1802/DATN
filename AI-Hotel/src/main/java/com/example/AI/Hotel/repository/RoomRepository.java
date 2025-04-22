package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomRepository extends JpaRepository<RoomType, Integer> {
    Page<RoomType> findAll(Pageable pageable); // phaan trang
    List<RoomType> findByHotelIdIn(List<Integer> hotelIds);



//    // Tìm phòng theo loại phòng (dựa trên name), số lượng khách, giá, và hotel_id
//    @Query("SELECT r FROM RoomType r WHERE " +
//            "(:roomType IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :roomType, '%'))) " +
//            "AND (:numberOfGuests IS NULL OR r.numberOfGuests >= :numberOfGuests) " +
//            "AND (:maxPrice IS NULL OR r.price <= :maxPrice) " +
//            "AND r.hotel.id = :hotelId")
//    List<RoomType> findByCriteriaAndHotelId(
//            @Param("roomType") String roomType,
//            @Param("numberOfGuests") Integer numberOfGuests,
//            @Param("maxPrice") Integer maxPrice,
//            @Param("hotelId") Integer hotelId
//    );
//
//    // Tìm phòng theo hotel_id
//    List<RoomType> findByHotelId(Integer hotelId);
}
