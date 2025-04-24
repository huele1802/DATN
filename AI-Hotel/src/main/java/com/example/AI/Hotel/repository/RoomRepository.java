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

    // truy vấn phòng theo giá và so luong khách
    @Query("SELECT rt FROM RoomType rt WHERE rt.price <= :maxPrice AND rt.numberOfGuests >= :numberOfGuests")
    List<RoomType> findByPriceAndGuests(
            @Param("maxPrice") Double maxPrice,
            @Param("numberOfGuests") Integer numberOfGuests);


}
