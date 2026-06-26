import json

def read_json_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    return data

data = read_json_file("output/hotels_cleaned.json")

mapping = {i: item['name'] for i, item in enumerate(data)}
with open('output/embedding_mapping.json', 'w', encoding='utf-8') as f:
    json.dump(mapping, f, ensure_ascii=False, indent=4)
print("Đã lưu mapping vào embedding_mapping.json")