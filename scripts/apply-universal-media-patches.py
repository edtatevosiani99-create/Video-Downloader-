from pathlib import Path

path = Path("app/src/main/java/com/videodownloader/app/MainActivity.java")
source = path.read_text(encoding="utf-8")

replacements = [
    (
        'String[] clients=youtube?(audioOnly?new String[]{"android_vr","web_embedded","default","tv","web_safari","android","ios","mweb",""}:new String[]{"android","web_safari","tv","web_embedded","default","ios","mweb",""}):(facebookSource?new String[]{"impersonate",""}:new String[]{""});',
        'String[] clients=youtube?(audioOnly?new String[]{"android_vr","web_embedded","default","tv","web_safari","android","ios","mweb",""}:new String[]{"android","web_safari","tv","web_embedded","default","ios","mweb",""}):(facebookSource?new String[]{"impersonate",""}:new String[]{"","impersonate",""});'
    ),
    (
        '                if(tiktok){\n                    // TikTok may reject the app\'s generic downloader user-agent.',
        '                if(!youtube&&!facebookSource&&!tiktok&&client.equals("impersonate")){\n                    // For other extractor-supported sites, retry as a normal Chromium browser.\n                    request.addOption("--impersonate","chrome");\n                    request.addOption("--user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");\n                    String scheme=Uri.parse(url).getScheme();\n                    String host=Uri.parse(url).getHost();\n                    if(scheme!=null&&host!=null)request.addOption("--referer",scheme+"://"+host+"/");\n                }\n                if(tiktok){\n                    // TikTok may reject the app\'s generic downloader user-agent.'
    ),
    (
        '                    request.addOption("-f","bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio")',
        '                    request.addOption("-f","bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best")'
    ),
    (
        '                    request.addOption("-f","best[ext=mp4][height<=720]/best[height<=720]/best[ext=mp4]/best");',
        '                    request.addOption("-f","best[ext=mp4]/best[height<=1080]/best");'
    ),
    (
        '                // Retry public YouTube extraction with another supported player client;\n                // do not loop on other sites or on a user-cancelled task.\n                if(!youtube&&!facebookSource)break;\n                if(viewUpdate!=null)viewUpdate.set(0,"Retrying YouTube extraction…");',
        '                // Retry extractor-supported sites with another client/header strategy.\n                // Do not stop after the first failure; many sites require browser impersonation.\n                if(viewUpdate!=null)viewUpdate.set(0,"Retrying media extraction…");'
    )
]

for old, new in replacements:
    if old not in source:
        raise SystemExit("Expected source fragment not found; refusing partial patch: " + old[:100])
    source = source.replace(old, new, 1)

path.write_text(source, encoding="utf-8")
print("Applied universal extractor retry matrix and broader format selectors.")
