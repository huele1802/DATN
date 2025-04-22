package com.example.AI.Hotel.service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.example.AI.Hotel.common.TextEmbeddingTranslator;
import com.example.AI.Hotel.model.*;
import com.example.AI.Hotel.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final PlaceRepository placeRepository;
    private final HotelEmbeddingRepository hotelEmbeddingRepository;
    private final RoomEmbeddingRepository roomEmbeddingRepository;
    private final PlaceEmbeddingRepository placeEmbeddingRepository;
    private final Predictor<String, float[]> predictor;

    @Autowired
    public EmbeddingService(
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            PlaceRepository placeRepository,
            HotelEmbeddingRepository hotelEmbeddingRepository,
            RoomEmbeddingRepository roomEmbeddingRepository,
            PlaceEmbeddingRepository placeEmbeddingRepository
    ) throws ModelException, IOException {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.placeRepository = placeRepository;
        this.hotelEmbeddingRepository = hotelEmbeddingRepository;
        this.roomEmbeddingRepository = roomEmbeddingRepository;
        this.placeEmbeddingRepository = placeEmbeddingRepository;

        logger.info("Initializing Sentence Transformers model...");
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/paraphrase-multilingual-mpnet-base-v2")
                .optEngine("PyTorch")
                .optOption("mapLocation", "true")
                .optOption("torchscript", "false")
                .optTranslator(new TextEmbeddingTranslator())
                .build();
        ZooModel<String, float[]> model = ModelZoo.loadModel(criteria);
        this.predictor = model.newPredictor(new TextEmbeddingTranslator());
        logger.info("Sentence Transformers model initialized successfully.");
    }
    @Bean
    public Predictor<String, float[]> predictor() {
        return predictor;
    }

    public boolean checkSourceDataExists() {
        boolean hasHotels = hotelRepository.count() > 0;
        boolean hasRooms = roomRepository.count() > 0;
        boolean hasPlaces = placeRepository.count() > 0;
        logger.info("Source data check - Hotels: {}, Rooms: {}, Places: {}", hasHotels, hasRooms, hasPlaces);
        return hasHotels || hasRooms || hasPlaces;
    }

    // đếm dữ liệu bản ghi đã được embedidng
    @Transactional
    public void precomputeEmbeddings(boolean forcePrecompute) {
        logger.info("Checking embedding status...");
        Map<String, Long> embeddingCounts = checkEmbeddingCounts();
        logger.info("Embedding counts - Hotels: {}, Rooms: {}, Places: {}",
                embeddingCounts.get("hotels"),
                embeddingCounts.get("rooms"),
                embeddingCounts.get("places"));
        if (!forcePrecompute && embeddingCounts.get("rooms") > 0 && embeddingCounts.get("hotels") > 0 && embeddingCounts.get("places") > 0) {
            logger.info("Embeddings already exist, skipping precomputation.");
        } else {
            logger.info("Precomputing embeddings...");
            precomputeHotelEmbeddings(forcePrecompute);
            logger.info("DONE precomputing hotel embeddings");
            precomputeRoomEmbeddings(forcePrecompute);
            logger.info("DONE precomputing room embeddings");
            precomputePlaceEmbeddings(forcePrecompute);
            logger.info("DONE precomputing place embeddings");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Long> checkEmbeddingCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("hotels", hotelEmbeddingRepository.count());
        counts.put("rooms", roomEmbeddingRepository.count());
        counts.put("places", placeEmbeddingRepository.count());
        return counts;
    }

    @Transactional
    public void precomputeHotelEmbeddings(boolean forcePrecompute) {
        logger.info("Precomputing embeddings for hotels...");
        long startTime = System.currentTimeMillis();
        List<Hotel> hotels = hotelRepository.findAll();
        logger.info("Found {} hotels to process.", hotels.size());

        for (Hotel hotel : hotels) {
            try {
                String text = buildHotelText(hotel);
                logger.debug("Hotel ID {}: Generated text: {}", hotel.getId(), text);
                if (text != null && !text.isEmpty()) {
                    float[] embedding = predictor.predict(text);
                    String embeddingString = arrayToString(embedding);
                    HotelEmbedding hotelEmbedding = new HotelEmbedding();
                    hotelEmbedding.setHotelId(hotel.getId());
                    hotelEmbedding.setTextEmbedding(embeddingString);
                    try {
                        hotelEmbeddingRepository.save(hotelEmbedding);
                        logger.error("TEST vector hotel {}", embeddingString.substring(0, 100) + "...");
                        logger.debug("Saved embedding for hotel ID {}.", hotel.getId());
                    } catch (Exception e) {
                        if (forcePrecompute) {
                            logger.warn("Duplicate hotel ID {} detected, skipping: {}", hotel.getId(), e.getMessage());
                        } else {
                            logger.error("Error saving embedding for hotel ID {}: {}", hotel.getId(), e.getMessage(), e);
                            throw e;
                        }
                    }
                } else {
                    logger.warn("Skipping hotel ID {}: Generated text is empty.", hotel.getId());
                }
            } catch (TranslateException e) {
                logger.error("Error embedding hotel ID {}: {}", hotel.getId(), e.getMessage(), e);
            }
        }
        logger.info("Finished precomputing embeddings for hotels in {} ms.", System.currentTimeMillis() - startTime);
    }

    @Transactional
    public void precomputeRoomEmbeddings(boolean forcePrecompute) {
        logger.info("Precomputing embeddings for rooms...");
        long startTime = System.currentTimeMillis();
        List<RoomType> rooms = roomRepository.findAll();
        logger.info("Found {} rooms to process.", rooms.size());

        for (RoomType room : rooms) {
            try {
                String text = buildRoomText(room);
                logger.debug("Room ID {}: Generated text: {}", room.getId(), text);
                if (text != null && !text.isEmpty()) {
                    float[] embedding = predictor.predict(text);
                    String embeddingString = arrayToString(embedding);
                    RoomEmbedding roomEmbedding = new RoomEmbedding();
                    roomEmbedding.setRoomId(room.getId());
                    roomEmbedding.setTextEmbedding(embeddingString);
                    try {
                        roomEmbeddingRepository.save(roomEmbedding);
                        logger.debug("Saved embedding for room ID {}.", room.getId());
                    } catch (Exception e) {
                        if (forcePrecompute) {
                            logger.warn("Duplicate room ID {} detected, skipping: {}", room.getId(), e.getMessage());
                        } else {
                            logger.error("Error saving embedding for room ID {}: {}", room.getId(), e.getMessage(), e);
                            throw e;
                        }
                    }
                } else {
                    logger.warn("Skipping room ID {}: Generated text is empty.", room.getId());
                }
            } catch (TranslateException e) {
                logger.error("Error embedding room ID {}: {}", room.getId(), e.getMessage(), e);
            }
        }
        logger.info("Finished precomputing embeddings for rooms in {} ms.", System.currentTimeMillis() - startTime);
    }

    @Transactional
    public void precomputePlaceEmbeddings(boolean forcePrecompute) {
        logger.info("Precomputing embeddings for places...");
        long startTime = System.currentTimeMillis();
        List<Place> places = placeRepository.findAll();
        logger.info("Found {} places to process.", places.size());

        for (Place place : places) {
            try {
                String text = buildPlaceText(place);
                logger.debug("Place ID {}: Generated text: {}", place.getId(), text);
                if (text != null && !text.isEmpty()) {
                    float[] embedding = predictor.predict(text);
                    String embeddingString = arrayToString(embedding);
                    PlaceEmbedding placeEmbedding = new PlaceEmbedding();
                    placeEmbedding.setPlaceId(place.getId());
                    placeEmbedding.setTextEmbedding(embeddingString);
                    try {
                        placeEmbeddingRepository.save(placeEmbedding);
                        logger.debug("Saved embedding for place ID {}.", place.getId());
                    } catch (Exception e) {
                        if (forcePrecompute) {
                            logger.warn("Duplicate place ID {} detected, skipping: {}", place.getId(), e.getMessage());
                        } else {
                            logger.error("Error saving embedding for place ID {}: {}", place.getId(), e.getMessage(), e);
                            throw e;
                        }
                    }
                } else {
                    logger.warn("Skipping place ID {}: Generated text is empty.", place.getId());
                }
            } catch (TranslateException e) {
                logger.error("Error embedding place ID {}: {}", place.getId(), e.getMessage(), e);
            }
        }
        logger.info("Finished precomputing embeddings for places in {} ms.", System.currentTimeMillis() - startTime);
    }

    private String buildHotelText(Hotel hotel) {
        StringBuilder text = new StringBuilder();
        if (hotel.getName() != null) {
            text.append(hotel.getName()).append(". ");
        }
        if (hotel.getDescription() != null) {
            text.append("Description: ").append(hotel.getDescription()).append(". ");
        }
        if (hotel.getFacilities() != null && !hotel.getFacilities().isEmpty()) {
            text.append("Facilities: ").append(String.join(", ", hotel.getFacilities())).append(". ");
        }
        if (hotel.getReviews() != null && !hotel.getReviews().isEmpty()) {
            text.append("Reviews: ");
            hotel.getReviews().forEach((key, value) -> text.append(key).append(": ").append(value).append(", "));
            text.setLength(text.length() - 2);
            text.append(". ");
        }
        if (hotel.getRatingStars() != null) {
            text.append("Rating: ").append(hotel.getRatingStars()).append(" stars. ");
        }
        if (hotel.getAddress() != null) {
            text.append("Address: ").append(hotel.getAddress()).append(". ");
        }
        if (hotel.getCoordinates() != null) {
            text.append("Located at: ").append(hotel.getCoordinates().getX()).append(",").append(hotel.getCoordinates().getY()).append(". ");
        }
        return text.toString().trim();
    }

    private String buildRoomText(RoomType room) {
        StringBuilder text = new StringBuilder();
        if (room.getName() != null) {
            text.append("Room type: ").append(room.getName()).append(". ");
        }
        if (room.getNumberOfGuests() != null) {
            text.append("Number of guests: ").append(room.getNumberOfGuests()).append(". ");
        }
        if (room.getPrice() != null) {
            text.append("Price: ").append(room.getPrice()).append(" VND. ");
        }
        Hotel hotel = room.getHotel();
        if (hotel != null) {
            if (hotel.getName() != null) {
                text.append("Hotel: ").append(hotel.getName()).append(". ");
            }
            if (hotel.getAddress() != null) {
                text.append("Address: ").append(hotel.getAddress()).append(". ");
            }
            if (hotel.getCoordinates() != null) {
                text.append("Located at: ").append(hotel.getCoordinates().getX()).append(",").append(hotel.getCoordinates().getY()).append(". ");
            }
            if (hotel.getRatingStars() != null) {
                text.append("Hotel rating: ").append(hotel.getRatingStars()).append(" stars. ");
            }
        }
        return text.toString().trim();
    }

    private String buildPlaceText(Place place) {
        StringBuilder text = new StringBuilder();
        if (place.getTitle() != null) {
            text.append(place.getTitle()).append(". ");
        }
        if (place.getRating() != null) {
            text.append("Rating: ").append(place.getRating()).append(". ");
        }
        if (place.getReviewCount() != null) {
            text.append("Review count: ").append(place.getReviewCount()).append(". ");
        }
        if (place.getAddress() != null) {
            text.append("Address: ").append(place.getAddress()).append(". ");
        }
        if (place.getCoordinates() != null) {
            text.append("Located at: ").append(place.getCoordinates().getX()).append(",").append(place.getCoordinates().getY()).append(". ");
        }
        if (place.getPhoneNumber() != null) {
            text.append("Phone number: ").append(place.getPhoneNumber()).append(". ");
        }
        return text.toString().trim();
    }



    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}