import json
import unicodedata
import re
import requests
from collections import defaultdict

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

def reverse_geocode_locationiq(lat, lon, api_key):
    url = f'https://us1.locationiq.com/v1/reverse.php'
    params = {
        'key': api_key,
        'lat': lat,
        'lon': lon,
        'format': 'json'
    }

    response = requests.get(url, params=params)
    if response.status_code == 200:
        data = response.json().get('address')
        district = data.get('suburb', '')
        if "District" in district:
            district = district.replace("District", "").strip()
        return district
    # else:
    #     return f"Lỗi: {response.status_code}"

# Đọc file JSON
with open('output3105/hotel_list.json', 'r', encoding='utf-8') as file:
    data = json.load(file)

api_key = 'pk.a8c92f700a43aaba591192d16c674b00'

hotel_list = []
existing_slugs = set()

for index, hotel in enumerate(data, 1):
    if hotel['rating_stars'] > 5:
        continue

    # 1. Chuẩn hóa văn bản
    hotel['name'] = hotel['name'].strip()
    hotel["slug"] = generate_slug(hotel["name"], existing_slugs)
    hotel['description'] = ' '.join(hotel['description'].split())  # Xóa khoảng trắng thừa

    hotel['address'] = hotel['address'].split('\n')[0]

    # 2. Tách tọa độ location
    hotel['latitude'], hotel['longitude'] = map(float, hotel['location'].split(','))
    hotel['district'] = reverse_geocode_locationiq(hotel['latitude'], hotel['longitude'], api_key)

    # 3. Chuẩn hóa highlights
    grouped_highlights = defaultdict(list)

    for item in hotel['highlights']:
        header = item["header"] if item['header'] != 'N/A' else 'Thông tin chung'
        grouped_highlights[header].extend(item["contents"])
    hotel['highlights'] = grouped_highlights

    # 4. Chuẩn hóa reviews
    reviews_dict = {}
    for review in hotel['reviews']:
        if ':' in review:
            key, value = review.split(':')
            reviews_dict[key.strip()] = float(value.replace(',', '.'))
    hotel['reviews'] = reviews_dict

    merged_room_services = {}
    for section in hotel['room_services']:
        merged_room_services.update(section)
    filtered_room_services = {k: v for k, v in merged_room_services.items() if v and k != "Ngôn ngữ được sử dụng"}
    hotel['room_services'] = filtered_room_services

    if not hotel.get('room_types'):
        continue

    # 5. Chuẩn hóa roomtypes
    for room in hotel['room_types']:
        room['taxes_and_fees_under_price'] = ( room['taxes_and_fees_under_price'] == "Đã bao gồm thuế và phí" )
        room['price'] = int(room['price'].replace('VND', '').replace('.', '').strip())
        if room['original_price'] == 'N/A':
            room['original_price'] = room['price']
        else:
            room['original_price'] = int(room['original_price'].replace('VND', '').replace('.', ''))

    hotel_list.append({
        k: v for k, v in hotel.items() if k not in ['location']
    })

# Lưu dữ liệu sang file JSON
with open('output3105/cleaned/hotels_cleaned.json', 'w', encoding='utf-8') as f:
    json.dump(hotel_list, f, ensure_ascii=False, indent=4)

# with open('output/roomtypes_cleaned.json', 'w', encoding='utf-8') as f:
#     json.dump(roomtype_list, f, ensure_ascii=False, indent=4)