import json
import os
import re

with open('parsed_errors.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

def read_lines(filepath):
    if not os.path.exists(filepath): return []
    with open(filepath, 'r', encoding='utf-8') as f:
        return f.readlines()

def write_lines(filepath, lines):
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(lines)

# 1. SpellCheckingInspection
spell_check_issues = data.get('SpellCheckingInspection', [])
words = set()
for issue in spell_check_issues:
    words.add(issue['highlight'].lower())

if words:
    os.makedirs('.idea/dictionaries', exist_ok=True)
    dict_xml = f"""<component name="ProjectDictionaryState">
  <dictionary name="fatih">
    <words>
{chr(10).join(f'      <w>{w}</w>' for w in sorted(words) if w)}
    </words>
  </dictionary>
</component>
"""
    with open('.idea/dictionaries/fatih.xml', 'w', encoding='utf-8') as f:
        f.write(dict_xml)
    print(f"Added {len(words)} words to dictionary.")

# 2. KotlinUnusedImport
unused_imports = data.get('KotlinUnusedImport', [])
files_to_fix = {}
for issue in unused_imports:
    f = issue['file']
    h = issue['highlight'].strip()
    if f not in files_to_fix:
        files_to_fix[f] = set()
    files_to_fix[f].add(h)

for f, imports in files_to_fix.items():
    if not os.path.exists(f): continue
    lines = read_lines(f)
    new_lines = []
    for line in lines:
        if line.strip() in imports:
            continue
        new_lines.append(line)
    write_lines(f, new_lines)
    print(f"Removed {len(imports)} unused imports from {f}")

# 3. AndroidLintUnusedResources
unused_res = data.get('AndroidLintUnusedResources', [])
for issue in unused_res:
    f = issue['file']
    h = issue['highlight'] # e.g. name="error_delete_failed"
    if not os.path.exists(f): continue
    lines = read_lines(f)
    new_lines = []
    for line in lines:
        if h in line:
            continue
        new_lines.append(line)
    write_lines(f, new_lines)
    print(f"Removed unused resource '{h}' from {f}")

# 4. UnusedVersionCatalogEntry
unused_cat = data.get('UnusedVersionCatalogEntry', [])
files_to_fix = {}
for issue in unused_cat:
    f = issue['file']
    h = issue['highlight'].strip() # e.g. alias("accompanist-systemuicontroller")
    if "alias(\"" in h:
        h = h.split('"')[1] # just the name
    elif h:
        # maybe it's just the name in highlight?
        pass
    
    if f not in files_to_fix:
        files_to_fix[f] = set()
    files_to_fix[f].add(h)

for f, entries in files_to_fix.items():
    if not os.path.exists(f): continue
    lines = read_lines(f)
    new_lines = []
    for line in lines:
        skip = False
        for e in entries:
            if e and e in line and not line.strip().startswith('#'):
                skip = True
                break
        if not skip:
            new_lines.append(line)
    write_lines(f, new_lines)
    print(f"Removed {len(entries)} unused catalog entries from {f}")

# 5. IgnoreFileDuplicateEntry
dup_entries = data.get('IgnoreFileDuplicateEntry', [])
for issue in dup_entries:
    f = issue['file']
    h = issue['highlight'].strip()
    if not os.path.exists(f): continue
    lines = read_lines(f)
    # just keep first occurrence
    new_lines = []
    seen = False
    for line in lines:
        if line.strip() == h:
            if not seen:
                seen = True
                new_lines.append(line)
            else:
                pass # remove duplicate
        else:
            new_lines.append(line)
    write_lines(f, new_lines)
    print(f"Removed duplicate entry '{h}' from {f}")

# 6. AndroidLintUseKtx
use_ktx = data.get('AndroidLintUseKtx', [])
for issue in use_ktx:
    f = issue['file']
    h = issue['highlight'].strip()
    if not os.path.exists(f): continue
    if h.startswith("Uri.parse("):
        inner = h[10:-1]
        new_h = f"{inner}.toUri()"
        lines = read_lines(f)
        new_lines = []
        for line in lines:
            if h in line:
                line = line.replace(h, new_h)
            new_lines.append(line)
        write_lines(f, new_lines)
        print(f"Fixed UseKtx in {f}")

# 7. RedundantSuppression
redundant_sup = data.get('RedundantSuppression', [])
for issue in redundant_sup:
    f = issue['file']
    h = issue['highlight'].strip()
    if not os.path.exists(f): continue
    lines = read_lines(f)
    new_lines = []
    for line in lines:
        if h in line:
            line = line.replace(h, "")
            if not line.strip(): # if empty now
                continue
        new_lines.append(line)
    write_lines(f, new_lines)
    print(f"Removed redundant suppression from {f}")

