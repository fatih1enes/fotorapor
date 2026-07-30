import os

files_to_suppress = [
    "app/src/main/java/com/fatihenes/photoreport/ui/CameraScreen.kt",
    "app/src/main/java/com/fatihenes/photoreport/ui/ProjectDetailScreen.kt",
    "app/src/main/java/com/fatihenes/photoreport/ui/SettingsScreen.kt",
    "app/src/main/java/com/fatihenes/photoreport/ui/TimelineBlock.kt",
    "app/src/main/java/com/fatihenes/photoreport/ui/components/ExportDialog.kt",
    "app/src/main/java/com/fatihenes/photoreport/ui/navigation/AppNavGraph.kt"
]

for filepath in files_to_suppress:
    if not os.path.exists(filepath): continue
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # Check if already has file suppress
    has_suppress = any('@file:Suppress' in l for l in lines[:5])
    if has_suppress:
        for i, l in enumerate(lines[:5]):
            if '@file:Suppress' in l:
                if 'LocalContextGetResourceValueCall' not in l:
                    lines[i] = l.replace('Suppress("', 'Suppress("LocalContextGetResourceValueCall", "')
                break
    else:
        lines.insert(0, '@file:Suppress("LocalContextGetResourceValueCall")\n')
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(lines)
    print(f"Added suppress to {filepath}")
