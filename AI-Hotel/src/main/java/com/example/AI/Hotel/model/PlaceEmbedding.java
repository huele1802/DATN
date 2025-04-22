package com.example.AI.Hotel.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;

@Entity
@Table(name = "place_embeddings_backup")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "place_id")
    private Integer placeId;


    @Column(name = "text_embedding", columnDefinition = "vector(768)")
    @Type(VectorType.class)
    private String textEmbedding;


}

