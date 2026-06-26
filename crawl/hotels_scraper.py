# # chỉ cào 3 thông tin của room
# from playwright.sync_api import sync_playwright
# import pandas as pd
# import time
# import csv
# import json
# import os

# def save_to_json_file(hotel_data, filename):
#     if os.path.exists(filename):
#         with open(filename, "r", encoding="utf-8") as f:
#             try:
#                 existing_data = json.load(f)
#             except json.JSONDecodeError:
#                 existing_data = []
#     else:
#         existing_data = []

#     existing_data.append(hotel_data)

#     with open(filename, "w", encoding="utf-8") as f:
#         json.dump(existing_data, f, ensure_ascii=False, indent=2)

# def crawl_link(url, headless=True):
#     with sync_playwright() as p:
#         browser = p.chromium.launch(headless=headless)
#         page = browser.new_page()
        
#         try:
#             page.goto(url, timeout=60000)

#             name = page.locator('h2.pp-header__title').inner_text().strip()

#             images_url = page.locator('//div[@data-testid="GalleryDesktop-wrapper"]//img').all()
#             img_list = [img.get_attribute('src') for img in images_url]

#             addressdestop = page.locator('//div[@data-testid="PropertyHeaderAddressDesktop-wrapper"]')
#             location = addressdestop.locator("div.fe4ce54ee2 a").first.get_attribute("data-atlas-latlng")
#             address = addressdestop.locator('div.a53cbfa6de').nth(0).inner_text().strip()
#             description = page.locator('//p[@data-testid="property-description"]').inner_text().strip()
#             facilities = page.locator('div.hp--popular_facilities span.a5a5a75131').all_inner_texts()

#             reviews = []
#             review_subscores = page.locator('//div[@role="group"]//div[@data-testid="review-subscore"]').all()
#             for subscore in review_subscores:
#                 divs = subscore.locator('div.ccb65902b2').all()
#                 text = f"{divs[0].locator('span').first.inner_text().strip()}:{divs[1].inner_text().strip()}"
#                 reviews.append(text)

#             rating_stars = len(page.locator('//span[@data-testid="rating-stars"]//span').all())

#             roomtypes = []
#             room_name = ""
#             roomtypes_link = page.locator('table.hprt-table tbody tr').all()
#             for link in roomtypes_link:
#                 room_name_candidates = link.locator('span.hprt-roomtype-icon-link').all()
#                 if room_name_candidates:
#                     room_name = room_name_candidates[0].inner_text()
                    
#                 bed_options = len(link.locator('i.bicon-occupancy').all())
#                 room_price = link.locator('span.prco-valign-middle-helper').inner_text()
#                 taxes_fees_under_price = link.locator('div.prd-taxes-and-fees-under-price').count() > 0
#                 original_price_locator = link.locator('div.bui-price-display__original')
#                 original_price = original_price_locator.inner_text().strip() if original_price_locator.count() > 0 else "N/A"

#                 roomtypes.append({
#                     'name': room_name,
#                     'number_of_guests': bed_options,
#                     'price': room_price,
#                     'taxes_and_fees_under_price': taxes_fees_under_price,
#                     'original_price': original_price
#                 })

#             highlights = []
#             li_elements = page.locator('div.property-highlights li.ph-section').all()
#             for li in li_elements:
#                 h4_key = get_header_text(li)
#                 span_elements = li.locator('span.ph-item-copy').all()
#                 span_contents = [span.inner_text().strip() for span in span_elements]
#                 highlights.append({
#                     "header": h4_key,
#                     "contents": span_contents
#                 })
            
#             # poi_data = {}
#             page.evaluate("window.scrollBy(0, document.body.scrollHeight)")
#             time.sleep(3)
            
#             # poi_blocks = page.locator('//div[@data-testid="poi-block"]').all()
#             # for poi_block in poi_blocks:
#             #     title = poi_block.locator('div.e1eebb6a1e').inner_text().strip()
#             #     li = poi_block.locator('//ul[@data-testid="poi-block-list"]//li').all()
#             #     place_list = []
#             #     for content in li:
#             #         place = content.locator('div.dc5041d860').inner_text().strip()
#             #         distance = content.locator('div.a53cbfa6de').inner_text().strip()
#             #         combine = f"{place} ({distance})"
#             #         place_list.append(combine)
#             #     poi_data[title] = place_list

#             services = []
#             room_service = page.locator('//div[@data-testid="facility-group-container"]').all()
#             for service in room_service:
#                 key = service.locator('h3 div.d1ca9115fe').inner_text().strip()
#                 # value = [sv.inner_text() for sv in service.locator('span.a5a5a75131').all()]
#                 span_locator = service.locator('span.a5a5a75131')
#                 value = []
#                 if span_locator.count() > 0:
#                     try:
#                         value = [sv.inner_text().strip() for sv in span_locator.all()]
#                     except Exception as e:
#                         print(f"Error extracting service text for {key}: {e}")
#                 services.append({key: value})

#             hotel_dict = {
#                 'hotel_link': url,
#                 'name': name,
#                 'location': location,
#                 'address': address,
#                 'description': description,
#                 'facilities': facilities,
#                 'highlights': highlights,
#                 'reviews': reviews,
#                 'room_types': roomtypes,
#                 'rating_stars': rating_stars,
#                 # 'surroundings': poi_data,
#                 "image_urls": img_list,
#                 'room_services': services
#             }
#             print("đã cào: ", hotel_dict.get('name'))
#             save_to_json_file(hotel_dict, 'output/hotel_list.json')
        
#         except Exception as e:
#             print(f"❌ Error with URL: {url} - {e}")
               
#         browser.close()

# def get_header_text(li):
#     try:
#         header = li.locator('h4.ph-item-header').inner_text().strip()
#         return header.strip() if header else "N/A"
#     except Exception:
#         return "N/A"
            
# if __name__ == '__main__':
#     hotels_url = []

#     with open('output/hotels_link.csv', newline='', encoding='utf-8') as csvfile:
#         reader = csv.DictReader(csvfile)
#         for row in reader:
#             hotels_url.append(row['hotel-url'])

#     for url in hotels_url:
#         crawl_link(url, headless=False)

# chỉ cào 3 thông tin của room



from playwright.sync_api import sync_playwright
import pandas as pd
import time
import csv
import json
import os

def save_to_json_file(hotel_data, filename):
    if os.path.exists(filename):
        with open(filename, "r", encoding="utf-8") as f:
            try:
                existing_data = json.load(f)
            except json.JSONDecodeError:
                existing_data = []
    else:
        existing_data = []

    existing_data.append(hotel_data)

    with open(filename, "w", encoding="utf-8") as f:
        json.dump(existing_data, f, ensure_ascii=False, indent=2)

def crawl_link(url, headless=True):
    with sync_playwright() as p:
        url_date = url + "&checkin=2025-06-29&checkout=2025-06-30"
        
        browser = p.chromium.launch(headless=headless)
        page = browser.new_page()
        
        try:
            page.goto(url_date, timeout=60000)

            name = page.locator('h2.pp-header__title').inner_text().strip()

            images_url = page.locator('//div[@data-testid="GalleryDesktop-wrapper"]//img').all()
            img_list = [img.get_attribute('src') for img in images_url]

            addressdestop = page.locator('//div[@data-testid="PropertyHeaderAddressDesktop-wrapper"]')
            location = addressdestop.locator("div a").first.get_attribute("data-atlas-latlng")
            address = addressdestop.locator('div.cb4b7a25d9').nth(0).inner_text().strip()
            description = page.locator('//p[@data-testid="property-description"]').inner_text().strip()
            facilities = page.locator('div.hp--popular_facilities span.f6b6d2a959').all_inner_texts()

            reviews = []
            review_subscores = page.locator('//div[@role="group"]//div[@data-testid="review-subscore"]').all()
            for subscore in review_subscores:
                divs = subscore.locator('div.ca9d921c46').all()
                text = f"{divs[0].locator('span.d96a4619c0').first.inner_text().strip()}:{divs[1].inner_text().strip()}"
                reviews.append(text)

            rating_stars = len(page.locator('//span[@data-testid="rating-stars"]//span').all())

            roomtypes = []
            room_name = ""
            roomtypes_link = page.locator('table.hprt-table tbody tr').all()
            for link in roomtypes_link:
                room_name_candidates = link.locator('span.hprt-roomtype-icon-link').all()
                if room_name_candidates:
                    room_name = room_name_candidates[0].inner_text()
                    
                bed_options = len(link.locator('i.bicon-occupancy').all())
                room_price = link.locator('span.prco-valign-middle-helper').inner_text()
                taxes_fees_under_price = link.locator('div.prd-taxes-and-fees-under-price').inner_text()
                # .count() > 0
                # .get_attribute("data-excl-charges-raw")
                original_price_locator = link.locator('div.bui-price-display__original')
                original_price = original_price_locator.inner_text().strip() if original_price_locator.count() > 0 else "N/A"

                roomtypes.append({
                    'name': room_name,
                    'number_of_guests': bed_options,
                    'price': room_price,
                    'taxes_and_fees_under_price': taxes_fees_under_price,
                    'original_price': original_price
                })

            highlights = []
            li_elements = page.locator('div.property-highlights li.ph-section').all()
            for li in li_elements:
                h4_key = get_header_text(li)
                span_elements = li.locator('span.ph-item-copy').all()
                span_contents = [span.inner_text().strip() for span in span_elements]
                highlights.append({
                    "header": h4_key,
                    "contents": span_contents
                })
            
            page.evaluate("window.scrollBy(0, document.body.scrollHeight)")
            time.sleep(3)

            services = []
            room_service = page.locator('//div[@data-testid="facility-group-container"]').all()
            for service in room_service:
                key = service.locator('h3 div.d31c9df771').inner_text().strip()
                span_locator = service.locator('span.f6b6d2a959')
                value = []
                if span_locator.count() > 0:
                    try:
                        value = [sv.inner_text().strip() for sv in span_locator.all()]
                    except Exception as e:
                        print(f"Error extracting service text for {key}: {e}")
                services.append({key: value})

            hotel_dict = {
                'hotel_link': url,
                'name': name,
                'location': location,
                'address': address,
                'description': description,
                'facilities': facilities,
                'highlights': highlights,
                'reviews': reviews,
                'room_types': roomtypes,
                'rating_stars': rating_stars,
                "image_urls": img_list,
                'room_services': services
            }
            print("đã cào: ", hotel_dict.get('name'))
            save_to_json_file(hotel_dict, 'output3105/hotel_list.json')
        
        except Exception as e:
            print(f"❌ Error with URL: {url} - {e}")
               
        browser.close()

def get_header_text(li):
    try:
        header = li.locator('h4.ph-item-header').inner_text().strip()
        return header.strip() if header else "N/A"
    except Exception:
        return "N/A"
            
if __name__ == '__main__':
    hotels_url = []

    with open('output3105/hotels_link.csv', newline='', encoding='utf-8') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            hotels_url.append(row['hotel-url'])

    for url in hotels_url:
        crawl_link(url, headless=False)