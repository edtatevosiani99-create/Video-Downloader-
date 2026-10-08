# Video Downloader

Android video/file downloader starter with a dark neon interface.

## Features

- Embedded browser with a URL field
- Direct HTTP/HTTPS file downloads
- Pre-download choice between video and MP3 audio for supported social-media links
- YouTube and public Facebook link extraction through yt-dlp where the platform allows access
- HLS master-playlist quality selection (resolution/bitrate) and concatenation of accessible, unencrypted MPEG-TS segments into a `.ts` video file
- Download progress, transfer speed, size information, and Android notifications
- Foreground download service keeps transfers running when the Activity is closed
- Download queue limits simultaneous transfers to two at a time
- Pause/resume and cancel controls
- Saves unfinished direct downloads and attempts to resume them when the app is reopened (when the server supports HTTP range requests)
- Share a URL from another app to open it in Video Downloader
- Extracts a compatible media file's existing audio track to an M4A file (not MP3 encoding)
- Local in-app library with open, share, and delete actions
- Seven interface languages: English, Russian, Georgian, Spanish, German, French, and Turkish
- Simple bottom advertising banner placeholder; no ad network or SDK is included

## Build APK

Push to `main` or open **Actions → Build Android APK → Run workflow**. The workflow builds `app-debug.apk` and uploads it as the `Video-Downloader-debug` artifact. Open the successful workflow run and download the artifact ZIP to retrieve the APK.

## Limitations

- Downloads work with direct HTTP/HTTPS file or media URLs that the source permits.
- A browser page is not the same as a direct media URL; many streaming services use segmented streams, authentication, or DRM and will not download through the direct-link downloader.
- YouTube and Facebook extraction depends on the source video being accessible to yt-dlp. Private, login-restricted, region-restricted, removed, or otherwise unavailable videos may fail. The app does not bypass DRM, sign-in, paywalls, or platform restrictions.
- Audio extraction decodes supported audio tracks and encodes MP3 with selectable 128/192/320 kbps bitrate. Some codecs, sample rates outside 8–48 kHz, or multichannel audio may not be supported by the encoder.
- HLS quality selection is limited to accessible playlists using unencrypted MPEG-TS segments. Encrypted/DRM streams, fragmented-MP4 HLS, and platform-specific protected streams are not supported.
- Downloads continue in a foreground service after the Activity is closed. If Android stops the app process or the device restarts, unfinished direct downloads are resumed when the app is opened again if the server supports byte-range requests.
- Downloaded files are stored in the app-specific Downloads directory. Share/open actions can pass files to other apps.

Only download content you own or are authorized to save.

## Third-party notices

- MP3 encoding uses [TAndroidLame](https://github.com/naman14/TAndroidLame), a LAME MP3 encoder wrapper licensed under GNU GPL v3. Review and comply with its license when distributing the application.
