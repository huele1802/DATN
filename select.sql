select * from hotel_embeddings;
select * from hotels;
select * from place_embeddings;
select * from places;
select * from room_types;


-- kiểm tra các vector trong cột embedding có đúng 768 chiều hay không(ko ra j là đúng)
SELECT 
    id,
    hotel_id,
    embedding,
    vector_dims(embedding) AS dimensions
FROM hotel_embeddings
WHERE vector_dims(embedding) != 768
   OR embedding IS NULL;

   SELECT id, embedding, vector_dims(embedding) AS dimensions
FROM hotel_embeddings
WHERE vector_dims(embedding) != 768
   OR embedding IS NULL
LIMIT 10;

SELECT id, embedding, vector_dims(embedding) AS dimensions
FROM place_embeddings
WHERE vector_dims(embedding) != 768
   OR embedding IS NULL
LIMIT 10;

SELECT table_name, column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_name IN ('hotels', 'places')
  AND column_name = 'address';

DELETE FROM hotel_embeddings WHERE vector_dims(embedding) != 768 OR embedding IS NULL;

SELECT id, facilities, highlights, reviews, image_urls, room_services
FROM hotels
LIMIT 10;