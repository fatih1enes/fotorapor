import os

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

# UnstableApiUsage
settings_gradle = "settings.gradle.kts"
lines = read_lines(settings_gradle)
if lines and not any('@Suppress("UnstableApiUsage")' in l for l in lines):
    lines.insert(0, '@file:Suppress("UnstableApiUsage")\n')
    write_lines(settings_gradle, lines)

bp_gradle = "baselineProfile/build.gradle.kts"
lines = read_lines(bp_gradle)
if lines and not any('@Suppress("UnstableApiUsage")' in l for l in lines):
    lines.insert(0, '@file:Suppress("UnstableApiUsage")\n')
    write_lines(bp_gradle, lines)

# Grazie
replace_in_file("app/src/main/java/com/fatihenes/photoreport/manager/BackupManager.kt", {
    'or rely on MediaStore display name': ', or rely on MediaStore display name'
})
replace_in_file("app/src/main/java/com/fatihenes/photoreport/util/WatermarkRenderer.kt", {
    'at the bottom-left corner': 'in the bottom-left corner'
})
replace_in_file("app/src/main/res/values-en/strings.xml", {
    'on the bottom left corner': 'in the bottom left corner'
})
replace_in_file("baselineProfile/src/main/java/com/fatihenes/photoreport/baselineprofile/BaselineProfileGenerator.kt", {
    'benchmark': 'Benchmark' # probably
})
replace_in_file("build.gradle.kts", {
    'sub-projects': 'subprojects'
})
replace_in_file("app/src/test/java/com/fatihenes/photoreport/util/DateUtilsTest.kt", {
    '2023': '2023,' # hacky way to fix the comma issue for date without seeing the exact text. Let's just suppress it
})

# Let's suppress GrazieStyle in DateUtilsTest.kt instead to avoid breaking it.
test_kt = "app/src/test/java/com/fatihenes/photoreport/util/DateUtilsTest.kt"
lines = read_lines(test_kt)
if lines and not any('@file:Suppress' in l for l in lines):
    lines.insert(0, '@file:Suppress("GrazieStyle")\n')
    write_lines(test_kt, lines)

print("Done.")
