import os
import xml.etree.ElementTree as ET
import json

errors_dir = r"c:\Users\fatih\Desktop\elektrik\hatalar"
results = []

for filename in os.listdir(errors_dir):
    if not filename.endswith(".xml") or filename in [".xml", "unused.xml", "index.html", "AndroidDomInspection.xml"]:
        continue
    filepath = os.path.join(errors_dir, filename)
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        if root.tag != 'problems':
            continue
        for problem in root.findall('problem'):
            file_node = problem.find('file')
            line_node = problem.find('line')
            class_node = problem.find('problem_class')
            desc_node = problem.find('description')
            highlight_node = problem.find('highlighted_element')
            
            f = file_node.text.replace('file://$PROJECT_DIR$/', '') if file_node is not None else 'Unknown'
            l = line_node.text if line_node is not None else '0'
            c = class_node.get('id') if class_node is not None else 'Unknown'
            d = desc_node.text if desc_node is not None else 'Unknown'
            h = highlight_node.text if highlight_node is not None else ''
            
            results.append({
                'rule': c,
                'file': f,
                'line': l,
                'highlight': h,
                'desc': d,
                'xml_file': filename
            })
    except Exception as e:
        print(f"Error parsing {filename}: {e}")

# Group by rule
grouped = {}
for r in results:
    rule = r['rule']
    if rule not in grouped:
        grouped[rule] = []
    grouped[rule].append(r)

with open(r"c:\Users\fatih\Desktop\elektrik\parsed_errors.json", "w", encoding="utf-8") as out:
    json.dump(grouped, out, indent=2)

print(f"Total rules found: {len(grouped)}")
for rule, items in grouped.items():
    print(f"{rule}: {len(items)} issues")
