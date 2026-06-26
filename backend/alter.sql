-- xác thực thêm dữ liệu embedding đã đc validate chính xác hay chưa
ALTER TABLE hotel_embeddings
ADD COLUMN is_validated BOOLEAN DEFAULT FALSE;

ALTER TABLE place_embeddings
ADD COLUMN is_validated BOOLEAN DEFAULT FALSE;

SELECT COUNT(*) FROM hotel_embeddings WHERE is_validated = TRUE; -- sau khi chạy mà dl ổn thì ra kết quả true