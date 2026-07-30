import os
import re

def read_lines(filepath):
    if not os.path.exists(filepath): return []
    with open(filepath, 'r', encoding='utf-8') as f:
        return f.readlines()

def write_lines(filepath, lines):
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(lines)

def replace_in_file(filepath, replacements):
    lines = read_lines(filepath)
    new_lines = []
    for line in lines:
        for old, new in replacements.items():
            if old in line:
                line = line.replace(old, new)
        new_lines.append(line)
    write_lines(filepath, new_lines)
    print(f"Applied replacements in {filepath}")

# AndroidLintHardcodedText in widget_photoreport.xml
widget_xml = "app/src/main/res/layout/widget_photoreport.xml"
replace_in_file(widget_xml, {
    'android:text="🏗️ Aktif Proje"': 'android:text="@string/widget_active_project"',
    'android:text="Proje Yok"': 'android:text="@string/widget_no_project"',
    'android:text="Görseller"': 'android:text="@string/widget_images"',
    'android:contentDescription="Kamera"': 'android:contentDescription="@string/widget_camera_desc"'
})

# Add strings to strings.xml
strings_xml = "app/src/main/res/values/strings.xml"
lines = read_lines(strings_xml)
new_lines = []
for line in lines:
    if '</resources>' in line:
        new_lines.append('    <string name="widget_active_project">🏗️ Aktif Proje</string>\n')
        new_lines.append('    <string name="widget_no_project">Proje Yok</string>\n')
        new_lines.append('    <string name="widget_images">Görseller</string>\n')
        new_lines.append('    <string name="widget_camera_desc">Kamera</string>\n')
    new_lines.append(line)
write_lines(strings_xml, new_lines)

# AndroidLintTypographyEllipsis
replace_in_file("app/src/main/res/values/strings.xml", {
    'Lütfen bekleyin...': 'Lütfen bekleyin…'
})
replace_in_file("app/src/main/res/values-en/strings.xml", {
    'Please wait...': 'Please wait…'
})

# AndroidLintUseCompatTextViewDrawableXml
replace_in_file(widget_xml, {
    'android:drawableTop="@drawable/ic_widget_gallery"': 'app:drawableTopCompat="@drawable/ic_widget_gallery"'
})
# Make sure app namespace is in widget layout if not
lines = read_lines(widget_xml)
has_app = any('xmlns:app="http://schemas.android.com/apk/res-auto"' in l for l in lines)
if not has_app:
    for i, l in enumerate(lines):
        if 'xmlns:android=' in l:
            lines.insert(i+1, '    xmlns:app="http://schemas.android.com/apk/res-auto"\n')
            break
write_lines(widget_xml, lines)

# AndroidLintRtlHardcoded
replace_in_file(widget_xml, {
    'android:paddingLeft="12dp"': 'android:paddingStart="12dp"',
    'android:paddingRight="8dp"': 'android:paddingEnd="8dp"',
    'android:layout_marginRight="4dp"': 'android:layout_marginEnd="4dp"'
})

# AndroidLintSmallSp
replace_in_file(widget_xml, {
    'android:textSize="10sp"': 'android:textSize="11sp"',
    'android:textSize="9sp"': 'android:textSize="11sp"'
})

# PrivatePropertyName
replace_in_file("app/src/androidTest/java/com/fatihenes/photoreport/data/MigrationTest.kt", {
    'TEST_DB': 'testDb'
})

# HtmlDeprecatedAttribute
replace_in_file("README.md", {
    '<h1 align="center">': '<h1>'
})

# AndroidLintTypos ("adres" -> "address"?) Wait, in Turkish strings.xml, "adres" is valid! Let's ignore it by adding tools:ignore="Typos"
replace_in_file("app/src/main/res/values/strings.xml", {
    '<string name="location_address">Adres: %1$s</string>': '<string name="location_address" tools:ignore="Typos">Adres: %1$s</string>'
})
# Also add xmlns:tools if missing in strings.xml
lines = read_lines(strings_xml)
has_tools = any('xmlns:tools="http://schemas.android.com/tools"' in l for l in lines)
if not has_tools:
    for i, l in enumerate(lines):
        if '<resources>' in l:
            lines[i] = '<resources xmlns:tools="http://schemas.android.com/tools">\n'
            break
write_lines(strings_xml, lines)

# AndroidLintUnusedAttribute (API 31 only)
replace_in_file("app/src/main/res/xml/widget_info_photoreport.xml", {
    'android:targetCellWidth': 'tools:targetApi="31"\n        android:targetCellWidth',
})
# add xmlns tools
lines = read_lines("app/src/main/res/xml/widget_info_photoreport.xml")
if not any('xmlns:tools' in l for l in lines):
    for i, l in enumerate(lines):
        if 'xmlns:android' in l:
            lines.insert(i+1, '    xmlns:tools="http://schemas.android.com/tools"\n')
            break
write_lines("app/src/main/res/xml/widget_info_photoreport.xml", lines)

# RemoveExplicitTypeArguments
replace_in_file("app/src/main/java/com/fatihenes/photoreport/ui/ProjectDetailScreen.kt", {
    'val scale = remember<Float>': 'val scale = remember',
    'val offsetX = remember<Float>': 'val offsetX = remember'
})

print("Done with auto replace script.")
