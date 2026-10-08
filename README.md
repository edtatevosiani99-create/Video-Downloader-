# Video Downloader

Android video/file downloader starter with a dark neon interface.

## Features

- Embedded browser with a URL field
- Direct HTTP/HTTPS file downloads
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
- YouTube or other protected media extraction is not implemented. The app does not bypass DRM, sign-in, paywalls, or platform restrictions.
- MP3 encoding/conversion and quality selection for streaming sources are not implemented. Audio extraction saves the existing audio track as M4A when the source format is compatible.
- Downloads continue in a foreground service after the Activity is closed. If Android stops the app process or the device restarts, unfinished direct downloads are resumed when the app is opened again if the server supports byte-range requests.
- Downloaded files are stored in the app-specific Downloads directory. Share/open actions can pass files to other apps.

Only download content you own or are authorized to save.
