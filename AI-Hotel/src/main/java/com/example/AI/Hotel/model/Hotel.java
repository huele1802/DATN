package com.example.AI.Hotel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "hotels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "hotel_link", nullable = false)
    private String hotelLink;

    @Column(name = "description", nullable = false)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "facilities", columnDefinition = "jsonb")
    private List<String> facilities;

    @Column(name = "rating_stars")
    private Integer ratingStars;

    private String address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reviews", columnDefinition = "jsonb")
    private Map<String, String> reviews;

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "surroundings", columnDefinition = "jsonb")
//    private Map<String, List<String>> surroundings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    private List<String> imageUrls;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point coordinates;

    // 1 khách sạn có nhiều phòng, 1 phòng chỉ có 1 khách sạn
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomType> roomTypes = new ArrayList<>();

}
