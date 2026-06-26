import json
import re
import unicodedata

def generate_slug(name, existing_slugs):
    # B1: Chuyển về lowercase và bỏ dấu tiếng Việt
    slug = unicodedata.normalize('NFD', name) \
        .encode('ascii', 'ignore') \
        .decode('utf-8') \
        .lower()
    
    # B2: Thay thế ký tự không hợp lệ bằng dấu gạch ngang
    slug = re.sub(r'[^a-z0-9]+', '-', slug).strip('-')

    # B3: Kiểm tra và thêm hậu tố nếu trùng
    original_slug = slug
    counter = 1
    while slug in existing_slugs:
        slug = f"{original_slug}-{counter}"
        counter += 1
    
    existing_slugs.add(slug)
    return slug

def clean_and_normalize_text(text):
    cleaned = re.sub(r'[^\w\s.,!?()-]', '', text)
    cleaned = cleaned.replace('\xa0', ' ').strip()
    cleaned = "\n".join(cleaned.split(" \n"))
    cleaned = cleaned.strip()
    return cleaned

def extract_description(des):
    description = ''
    service = []
    cleaned = clean_and_normalize_text(des)
    service_txt = cleaned
    if "\n\n\n\n" in cleaned:
        description = cleaned.split('\n\n\n\n')[0]
        service_txt = cleaned.split('\n\n\n\n')[1]

    contents = [service_txt]
    if '\n\n\n' in service_txt:
        contents = service_txt.split('\n\n\n')

    for content in contents:
        if '\n\n' in content:
            list = content.split('\n\n')
            service.append({list[0]: list[1:]})
    
    return description, service


with open('C:/Users/ADMIN/Documents/Zalo Received Files/địa điểm du lịch.json', 'r', encoding='utf-8') as file:
    data = json.load(file)

place_list = []
existing_slugs = set()

for place in data:
    place['title'] = place['title'].strip()
    place["slug"] = generate_slug(place["title"], existing_slugs)
    place['description'], place['service'] = extract_description(place['decription'])
    place['rating'] = float(place['rating'].strip().replace(',', '.'))
    place['review'] = int(place['review'].strip().replace('.', ''))
    place['address'] = place['address'].strip()
    place['latitude'] = float(place['lattitude'])
    place['longitude'] = float(place['longtitude'])

    place_list.append({
        k: v for k, v in place.items() if k not in ['decription', 'lattitude', 'longtitude']
    })

# Lưu dữ liệu sang file JSON
with open('output/places_cleaned.json', 'w', encoding='utf-8') as f:
    json.dump(place_list, f, ensure_ascii=False, indent=4)