package com.example.AI.Hotel.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;

@Entity
@Table(name = "hotel_embeddings_backup")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hotel_id")
    private Integer hotelId;

    @Column(name = "text_embedding", columnDefinition = "vector(768)")
    @Type(VectorType.class)
    private String textEmbedding;


}

