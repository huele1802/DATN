
CREATE INDEX idx_hotels_name ON hotels(name);
CREATE INDEX idx_hotels_district ON hotels(district);
CREATE INDEX idx_room_types_hotel_id ON room_types(hotel_id);
CREATE INDEX idx_places_title ON places(title);
CREATE INDEX idx_hotel_embeddings_hotel_id ON hotel_embeddings(hotel_id);
CREATE INDEX idx_place_embeddings_place_id ON place_embeddings(place_id);

-- tăng tốc tìm kiếm tương đồng
CREATE INDEX idx_hotel_embeddings_embedding ON hotel_embeddings USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX idx_place_embeddings_embedding ON place_embeddings USING ivfflat (embedding vector_cosine_ops);

SET maintenance_work_mem = '64MB';



CREATE INDEX idx_hotels_coordinates ON hotels USING GIST (coordinates);
CREATE INDEX idx_places_coordinates ON places USING GIST (coordinates);