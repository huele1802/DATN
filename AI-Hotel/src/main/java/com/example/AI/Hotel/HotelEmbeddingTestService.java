//package com.example.AI.Hotel;
//
//import com.example.AI.Hotel.model.Hotel;
//import com.example.AI.Hotel.model.HotelEmbedding;
//import com.example.AI.Hotel.repository.HotelEmbeddingRepository;
//import com.example.AI.Hotel.repository.HotelRepository;
//import com.example.AI.Hotel.service.EmbeddingService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@SpringBootApplication
//public class HotelEmbeddingTestService {
//
//    private static final Logger logger = LoggerFactory.getLogger(HotelEmbeddingTestService.class);
//
//    private final HotelRepository hotelRepository;
//    private final HotelEmbeddingRepository hotelEmbeddingRepository;
//    private final EmbeddingService embeddingService;
//
//    @Autowired
//    public HotelEmbeddingTestService(
//            HotelRepository hotelRepository,
//            HotelEmbeddingRepository hotelEmbeddingRepository,
//            EmbeddingService embeddingService
//    ) {
//        this.hotelRepository = hotelRepository;
//        this.hotelEmbeddingRepository = hotelEmbeddingRepository;
//        this.embeddingService = embeddingService;
//    }
//
//    @Transactional
//    public void testPrecomputeHotelEmbeddings(boolean forcePrecompute) {
//        logger.info("Starting test for precomputeHotelEmbeddings...");
//
//        // Bước 1: Kiểm tra dữ liệu khách sạn trong database
//        List<Hotel> hotels = hotelRepository.findAll();
//        logger.info("Found {} hotels in database.", hotels.size());
//        if (hotels.isEmpty()) {
//            logger.warn("No hotels found in database. Please add hotel data before testing.");
//            return;
//        }
//
//        // Bước 2: Xóa embedding cũ (nếu forcePrecompute = true)
//        if (forcePrecompute) {
//            logger.info("Clearing existing hotel embeddings...");
//            hotelEmbeddingRepository.deleteAll();
//            logger.info("Hotel embeddings cleared.");
//        }
//
//        // Bước 3: Gọi precomputeHotelEmbeddings
//        try {
//            embeddingService.precomputeHotelEmbeddings(forcePrecompute);
//            logger.info("precomputeHotelEmbeddings executed successfully.");
//        } catch (Exception e) {
//            logger.error("precomputeHotelEmbeddings failed: {}", e.getMessage(), e);
//            return;
//        }
//
//        // Bước 4: Kiểm tra kết quả trong database
//        List<HotelEmbedding> embeddings = hotelEmbeddingRepository.findAll();
//        logger.info("Found {} hotel embeddings in database.", embeddings.size());
//
//        int validEmbeddings = 0;
//        int invalidEmbeddings = 0;
//
//        for (HotelEmbedding embedding : embeddings) {
//            try {
//                float[] vector = embedding.getTextEmbedding();
//                if (vector == null) {
//                    logger.warn("Hotel ID {}: Embedding is null.", embedding.getHotelId());
//                    invalidEmbeddings++;
//                    continue;
//                }
//                if (vector.length == 768) {
//                    logger.info("Hotel ID {}: Valid embedding with length 768.", embedding.getHotelId());
//                    validEmbeddings++;
//                } else {
//                    logger.warn("Hotel ID {}: Invalid embedding length: {}.", embedding.getHotelId(), vector.length);
//                    invalidEmbeddings++;
//                }
//            } catch (Exception e) {
//                logger.error("Error checking embedding for hotel ID {}: {}", embedding.getHotelId(), e.getMessage());
//                invalidEmbeddings++;
//            }
//        }
//
//        // Bước 5: In kết quả tổng kết
//        logger.info("Test completed. Results:");
//        logger.info("Total hotels processed: {}", hotels.size());
//        logger.info("Valid embeddings (length 768): {}", validEmbeddings);
//        logger.info("Invalid embeddings: {}", invalidEmbeddings);
//        if (invalidEmbeddings > 0) {
//            logger.warn("Some embeddings are invalid. Check logs for details.");
//        } else if (validEmbeddings == hotels.size()) {
//            logger.info("All embeddings are valid. precomputeHotelEmbeddings is working correctly!");
//        } else {
//            logger.warn("Some hotels were not embedded. Check database and logs.");
//        }
//    }
//
//    @Transactional
//    public void testSingleHotelEmbedding(Integer hotelId) {
//        logger.info("Testing embedding for hotel ID {}...", hotelId);
//
//        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
//        if (hotel == null) {
//            logger.error("Hotel ID {} not found in database.", hotelId);
//            return;
//        }
//
//        try {
//            // Tạo văn bản từ hotel
//            String text = invokeBuildHotelText(hotel);
//            logger.info("Generated text for hotel ID {}: {}", hotelId, text);
//
//            if (text == null || text.trim().isEmpty()) {
//                logger.error("Text for hotel ID {} is empty or null.", hotelId);
//                return;
//            }
//
//            // Tạo embedding
//            float[] embedding = embeddingService.embedText(text);
//            logger.info("Embedding for hotel ID {}: length = {}", hotelId, embedding.length);
//
//            // Kiểm tra embedding
//            if (embedding.length == 768) {
//                logger.info("Valid embedding generated for hotel ID {}.", hotelId);
//            } else {
//                logger.warn("Invalid embedding length for hotel ID {}: {}.", hotelId, embedding.length);
//            }
//
//            // Lưu embedding
//            HotelEmbedding hotelEmbedding = new HotelEmbedding();
//            hotelEmbedding.setHotelId(hotelId);
//            hotelEmbedding.setTextEmbedding(embedding);
//            hotelEmbeddingRepository.save(hotelEmbedding);
//            logger.info("Embedding saved for hotel ID {}.", hotelId);
//
//        } catch (Exception e) {
//            logger.error("Failed to generate or save embedding for hotel ID {}: {}", hotelId, e.getMessage(), e);
//        }
//    }
//
//    private String invokeBuildHotelText(Hotel hotel) {
//        StringBuilder text = new StringBuilder();
//        if (hotel.getName() != null) {
//            text.append(hotel.getName()).append(". ");
//        }
//        if (hotel.getDescription() != null) {
//            String desc = hotel.getDescription().length() > 100 ?
//                    hotel.getDescription().substring(0, 100) + "..." : hotel.getDescription();
//            text.append("Description: ").append(desc).append(". ");
//        }
//        if (hotel.getFacilities() != null && !hotel.getFacilities().isEmpty()) {
//            text.append("Facilities: ").append(String.join(", ", hotel.getFacilities())).append(". ");
//        }
//        if (hotel.getReviews() != null && !hotel.getReviews().isEmpty()) {
//            text.append("Reviews: ");
//            hotel.getReviews().forEach((key, value) -> text.append(key).append(": ").append(value).append(", "));
//            text.setLength(text.length() - 2);
//            text.append(". ");
//        }
//        if (hotel.getRatingStars() != null) {
//            text.append("Rating: ").append(hotel.getRatingStars()).append(" stars. ");
//        }
//        if (hotel.getAddress() != null) {
//            text.append("Address: ").append(hotel.getAddress()).append(". ");
//        }
//        if (hotel.getCoordinates() != null) {
//            text.append("Located at: ").append(hotel.getCoordinates().getX()).append(",").append(hotel.getCoordinates().getY()).append(". ");
//        }
//        String result = text.toString().trim();
//        logger.debug("Hotel text length: {}", result.length());
//        return result;
//    }
//
//    public static void main(String[] args) {
//        logger.info("Starting HotelEmbeddingTestService...");
//
//        // Khởi tạo Spring Application Context
//        ApplicationContext context = SpringApplication.run(HotelEmbeddingTestService.class, args);
//        HotelEmbeddingTestService testService = context.getBean(HotelEmbeddingTestService.class);
//
//        try {
//            // Kiểm tra toàn bộ khách sạn
//            logger.info("Running testPrecomputeHotelEmbeddings...");
//            testService.testPrecomputeHotelEmbeddings(true);
//
//            // Kiểm tra một khách sạn cụ thể (thay 1L bằng ID thực tế trong database)
//            logger.info("Running testSingleHotelEmbedding for hotel ID 1...");
////            testService.testSingleHotelEmbedding(1L);
//
//        } catch (Exception e) {
//            logger.error("Test failed: {}", e.getMessage(), e);
//        } finally {
//            // Đóng context
//            SpringApplication.exit(context);
//            logger.info("HotelEmbeddingTestService completed.");
//        }
//    }
//}