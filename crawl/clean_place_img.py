import json
import re
import unicodedata
import cloudinary
import cloudinary.uploader
import cloudinary.api

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

def upload_image_from_url(image_url):
    try:
        result = cloudinary.uploader.upload(image_url, folder="HotelProposal/places")
        print("Upload thành công:", result["secure_url"])
        return result["secure_url"]
    except Exception as e:
        print("Lỗi upload:", e)
        return None

# Cấu hình tài khoản Cloudinary
cloudinary.config(
    cloud_name="doityourself",
    api_key="634821489138164",
    api_secret="kH88Q6HrCa5srFzIVjoZkcQ6usU"
)

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
    place['img'] = upload_image_from_url(place['img'])

    place_list.append({
        k: v for k, v in place.items() if k not in ['decription', 'lattitude', 'longtitude']
    })

# Lưu dữ liệu sang file JSON
with open('output/cleaned/places_img_cleaned.json', 'w', encoding='utf-8') as f:
    json.dump(place_list, f, ensure_ascii=False, indent=4)




# import json
# import unicodedata
# import re
# import cloudinary
# import cloudinary.uploader
# from concurrent.futures import ThreadPoolExecutor, as_completed

# cloudinary.config(
#     cloud_name="namw",
#     api_key="key",
#     api_secret="secret"
# )

# def generate_slug(name, existing_slugs):
#     slug = unicodedata.normalize('NFD', name).encode('ascii', 'ignore').decode('utf-8').lower()
#     slug = re.sub(r'[^a-z0-9]+', '-', slug).strip('-')
#     original_slug = slug
#     counter = 1
#     while slug in existing_slugs:
#         slug = f"{original_slug}-{counter}"
#         counter += 1
#     existing_slugs.add(slug)
#     return slug

# def clean_and_normalize_text(text):
#     cleaned = re.sub(r'[^\w\s.,!?()-]', '', text).replace('\xa0', ' ').strip()
#     cleaned = "\n".join(cleaned.split(" \n")).strip()
#     return cleaned

# def extract_description(des):
#     description, service = '', []
#     cleaned = clean_and_normalize_text(des)
#     service_txt = cleaned
#     if "\n\n\n\n" in cleaned:
#         description, service_txt = cleaned.split('\n\n\n\n', 1)

#     contents = service_txt.split('\n\n\n') if '\n\n\n' in service_txt else [service_txt]

#     for content in contents:
#         if '\n\n' in content:
#             lines = content.split('\n\n')
#             service.append({lines[0]: lines[1:]})
#     return description, service

# def upload_image_from_url(image_url):
#     try:
#         result = cloudinary.uploader.upload(image_url, folder="HotelProposal/places")
#         return result["secure_url"]
#     except Exception as e:
#         print(f"Lỗi upload {image_url}:", e)
#         return None

# # Load dữ liệu từ file
# with open('C:/Users/ADMIN/Documents/Zalo Received Files/địa điểm du lịch.json', 'r', encoding='utf-8') as file:
#     data = json.load(file)

# place_list = []
# existing_slugs = set()

# # Bước 1: Tiền xử lý dữ liệu, tách riêng danh sách upload
# upload_tasks = []

# for place in data:
#     place['title'] = place['title'].strip()
#     place["slug"] = generate_slug(place["title"], existing_slugs)
#     place['description'], place['service'] = extract_description(place['decription'])
#     place['rating'] = float(place['rating'].strip().replace(',', '.'))
#     place['review'] = int(place['review'].strip().replace('.', ''))
#     place['address'] = place['address'].strip()
#     place['latitude'] = float(place['lattitude'])
#     place['longitude'] = float(place['longtitude'])
#     upload_tasks.append((place, place['img']))  # lưu lại để xử lý song song

# # Bước 2: Dùng đa luồng để upload ảnh
# with ThreadPoolExecutor(max_workers=10) as executor:
#     future_to_place = {executor.submit(upload_image_from_url, img): place for place, img in upload_tasks}
#     for future in as_completed(future_to_place):
#         place = future_to_place[future]
#         place['img'] = future.result()
#         place_list.append({
#             k: v for k, v in place.items() if k not in ['decription', 'lattitude', 'longtitude']
#         })

# # Bước 3: Ghi ra file
# with open('output/cleaned/places_img_cleaned.json', 'w', encoding='utf-8') as f:
#     json.dump(place_list, f, ensure_ascii=False, indent=4)
