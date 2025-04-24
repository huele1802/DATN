CREATE INDEX idx_hotels_coordinates ON hotels USING GIST (coordinates);
CREATE INDEX idx_places_coordinates ON places USING GIST (coordinates);