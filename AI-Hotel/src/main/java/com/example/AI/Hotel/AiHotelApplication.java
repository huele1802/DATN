package com.example.AI.Hotel;

import com.example.AI.Hotel.repository.HotelEmbeddingRepository;
import com.example.AI.Hotel.repository.PlaceEmbeddingRepository;
import com.example.AI.Hotel.repository.RoomEmbeddingRepository;
import com.example.AI.Hotel.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
public class AiHotelApplication {

	private final EmbeddingService embeddingService;
	private static final Logger log = LoggerFactory.getLogger(AiHotelApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AiHotelApplication.class, args);
	}

	@Bean
	public CommandLineRunner init(EmbeddingService embeddingService) {
		return args -> {
			boolean forcePrecompute = false;
			// Kiểm tra tham số khởi động
			for (String arg : args) {
				if (arg.equals("--precompute=true")) {
					forcePrecompute = true;
					break;
				}
			}

			try {
				// Gọi phương thức từ EmbeddingService để kiểm tra và precompute
				boolean dataExists = embeddingService.checkSourceDataExists();
				log.info("Source data exists? {}", dataExists);

				if (forcePrecompute || dataExists) {
					log.info("Starting precompute embeddings... (forcePrecompute: {}, dataExists: {})",
							forcePrecompute, dataExists);
					embeddingService.precomputeEmbeddings(forcePrecompute);
					log.info("Successfully precomputed embeddings for hotels, rooms, and places.");
				} else {
					log.info("No source data found in hotels, room_types, or places, skipping precompute.");
				}
			} catch (Exception e) {
				log.error("Error during precompute embeddings: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to precompute embeddings", e);
			}
		};
	}
}