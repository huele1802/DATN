package com.example.AI.Hotel.repository;

import com.example.AI.Hotel.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomRepository extends JpaRepository<RoomType, Integer> {
    Page<RoomType> findAll(Pageable pageable); // phân trang
    List<RoomType> findByHotelIdIn(List<Integer> hotelIds);

    //  tìm phòng chỉ theo giá
    @Query("SELECT r FROM RoomType r WHERE r.price <= :maxPrice")
    List<RoomType> findByPrice(@Param("maxPrice") Double maxPrice);

    // tìm phòng chỉ theo số khách
    @Query("SELECT r FROM RoomType r WHERE r.numberOfGuests >= :numberOfGuests")
    List<RoomType> findByGuests(@Param("numberOfGuests") Integer numberOfGuests);

    // truy vấn phòng theo giá và so luong khách
    @Query("SELECT rt FROM RoomType rt WHERE rt.price <= :maxPrice AND rt.numberOfGuests >= :numberOfGuests")
    List<RoomType> findByPriceAndGuests(
            @Param("maxPrice") Double maxPrice,
            @Param("numberOfGuests") Integer numberOfGuests);

    @Query("SELECT rt FROM RoomType rt WHERE rt.hotel.id = :hotelId")
    List<RoomType> findByHotelId(@Param("hotelId") Integer hotelId);

}
