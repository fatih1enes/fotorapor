import json
with open('parsed_errors.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
for i in data.get('AndroidLintLocalContextGetResourceValueCall', []):
    print(f"{i['file']}:{i['line']} -> {i['highlight']}")
