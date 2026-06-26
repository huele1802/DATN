import requests
import json

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
        print(data)
        road = data.get('road', '')
        quarter = data.get('quarter', '')
        district = data.get('suburb', '')
        
        address = f"{road}, {quarter}, {district}"
        return address
    else:
        return f"Lỗi: {response.status_code}"
    
# Đọc file JSON
def read_json_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    return data
# Gọi thử
api_key = 'pk.a8c92f700a43aaba591192d16c674b00'

json_file = 'output/hotels_cleaned.json'
data = read_json_file(json_file)

for hotel in data:
    data['district'] = reverse_geocode_locationiq(hotel['latitude'], hotel['longitude'], api_key)
# print(reverse_geocode_locationiq(16.071629739941834, 108.23499803684854, api_key))

# def geocode_place_name(place_name, api_key):
#     url = 'https://us1.locationiq.com/v1/search.php'
#     params = {
#         'key': api_key,
#         'q': place_name,
#         'format': 'json'
#     }

#     response = requests.get(url, params=params)

#     if response.status_code == 200:
#         data = response.json()
#         if data:
#             first_result = data[0]
#             display_name = first_result.get('display_name')
#             lat = first_result.get('lat')
#             lon = first_result.get('lon')
#             return first_result, lat, lon
#         else:
#             return "Không tìm thấy", None, None
#     else:
#         return f"Lỗi: {response.status_code}", None, None

# api_key = 'pk.a8c92f700a43aaba591192d16c674b00'
# place = "Platinum Orchid Hotel Danang"

# address, lat, lon = geocode_place_name(place, api_key)

# if lat and lon:
#     print(f"📍 Địa chỉ: {address}")
#     print(f"🌐 Tọa độ: {lat}, {lon}")
# else:
#     print(address)