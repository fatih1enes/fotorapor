import json

with open('parsed_errors.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

rules = [
    'AndroidLintHardcodedText', 'AndroidLintTypographyEllipsis', 
    'AndroidLintUseCompatTextViewDrawableXml', 'RemoveExplicitTypeArguments', 
    'UnusedSymbol', 'UnusedVariable', 'AndroidLintRemoveWorkManagerInitializer', 
    'PrivatePropertyName', 'HtmlDeprecatedAttribute', 'AndroidLintForegroundServicesPolicy', 
    'AndroidLintInlinedApi', 'AndroidLintRtlHardcoded', 'AndroidLintSmallSp', 
    'AndroidLintTypos', 'AndroidLintUnusedAttribute', 'UnstableApiUsage'
]

with open('remaining_issues.txt', 'w', encoding='utf-8') as out:
    for r in rules:
        out.write(f'\n--- {r} ---\n')
        for i in data.get(r, []):
            out.write(f"{i['file']}:{i['line']} -> {i['highlight']} ({i['desc']})\n")
