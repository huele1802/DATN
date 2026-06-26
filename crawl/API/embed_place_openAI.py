import os

from openai import OpenAI
import json
import numpy as np
import time
from tqdm import tqdm

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def convert_place_to_text(data):
    # Lấy phần dịch vụ
    services = []
    for s in data.get("service", []):
        for k, v in s.items():
            services.append(f"{k}: {', '.join(v)}")
    services_text = " | ".join(services)

    text_embedding = f"{data.get('title', '')} nằm ở {data.get('address', '')}, được đánh giá {data.get('rating', 0)} sao với {data.get('review', 0)} lượt đánh giá. {data.get('description', '')}. Dịch vụ: {services_text}."
    return text_embedding

# Đọc file JSON
def read_json_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    return data

# ----- Chuyển text -> embedding dùng OpenAI -----
def get_openai_embedding(text, model="text-embedding-3-large", retry_delay=2, max_retries=5):
    for attempt in range(max_retries):
        try:
            response = client.embeddings.create(input=[text], model=model)
            return response.data[0].embedding
        except Exception as e:
            print(f"Lỗi embedding (lần thử {attempt + 1}): {e}")
            time.sleep(retry_delay + attempt)  # tăng dần thời gian chờ
    raise RuntimeError("❌ Quá số lần retry embedding")

def text_to_embeddings_openai(texts, delay_every=30, delay_seconds=1):
    embeddings = []
    for i, text in enumerate(tqdm(texts, desc="Embedding")):
        embedding = get_openai_embedding(text)
        embeddings.append(embedding)
        if (i + 1) % delay_every == 0:
            time.sleep(delay_seconds)
    return np.array(embeddings)

# Lưu kết quả embeddings vào file (tuỳ chọn)
def save_embeddings(embeddings, output_file):
    np.save(output_file, embeddings)

# Hàm chính
def main(json_file, output_file):
    data = read_json_file(json_file)
    texts = [convert_place_to_text(item) for item in data]
    embeddings = text_to_embeddings_openai(texts)
    save_embeddings(embeddings, output_file)
    
    print(f"Đã tạo và lưu {len(embeddings)} embeddings vào {output_file}")

if __name__ == "__main__":
    json_file = 'output/cleaned/places_img_cleaned.json'
    output_file = 'output3105/embedding/place_embeddings_openai.npy'
    
    main(json_file, output_file)