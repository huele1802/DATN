package com.example.AI.Hotel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "room_embeddings_backup")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "hotel_id")  // Thêm trường hotelId
    private Integer hotelId;

    @Column(name = "text_embedding", columnDefinition = "vector(768)")
    @Type(VectorType.class)
    private String textEmbedding;
}
