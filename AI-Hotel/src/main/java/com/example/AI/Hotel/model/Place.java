package com.example.AI.Hotel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "places")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "rating", length = 50)
    private String rating;

    @Column(name = "review_count", columnDefinition = "TEXT")
    private String reviewCount;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

//    @Type(org.hibernate.spatial.GeometryType.class)
    @Column(name = "coordinates", columnDefinition = "geography(POINT, 4326)")
    private Point coordinates;
}
