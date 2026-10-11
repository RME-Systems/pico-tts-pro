import urllib.request
import json
import os
import sys

# Official Emojibase compact datasets (Unicode / CLDR standard)
# Includes all root emojis AND all skin tone/gender/composite variations (3,900+ total)
LOCALES = ['es', 'en', 'fr', 'de', 'it']
BASE_URL = 'https://cdn.jsdelivr.net/npm/emojibase-data@latest/{locale}/compact.json'

def download_and_generate():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    target_dir = os.path.join(project_root, 'app', 'src', 'main', 'assets', 'emojis')
    os.makedirs(target_dir, exist_ok=True)

    print("Fetching COMPLETE Unicode emoji annotations (including all skin tone variations)...")
    for loc in LOCALES:
        url = BASE_URL.format(locale=loc)
        print(f"Downloading {loc} from {url}...")
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=25) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                
            # Filter and create clean key-value mapping: emoji -> label
            mapping = {}
            for item in data:
                # 1. Base / root emoji
                emoji = item.get('unicode') or item.get('emoji')
                label = item.get('label')
                if emoji and label:
                    mapping[emoji] = label
                
                # 2. All skin tone & modifier variations
                skins = item.get('skins', [])
                for skin in skins:
                    skin_emoji = skin.get('unicode') or skin.get('emoji')
                    skin_label = skin.get('label')
                    if skin_emoji and skin_label:
                        mapping[skin_emoji] = skin_label

            out_file = os.path.join(target_dir, f'emojis_{loc}.json')
            with open(out_file, 'w', encoding='utf-8') as f:
                json.dump(mapping, f, ensure_ascii=False, separators=(',', ':'))
                
            print(f"Saved {len(mapping)} emojis for '{loc}' -> {out_file} ({os.path.getsize(out_file) // 1024} KB)")
        except Exception as e:
            print(f"Error downloading {loc}: {e}", file=sys.stderr)

if __name__ == '__main__':
    download_and_generate()
