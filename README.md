# Video Downloader

Android video/file downloader starter with a dark neon interface.

## Features

- Embedded browser with a URL field
- Direct HTTP/HTTPS file downloads
- Download progress and Android notifications
- In-session pause/resume and cancel controls
- Local in-app library with open, share, and delete actions
- Seven interface languages: English, Russian, Georgian, Spanish, German, French, and Turkish
- Bottom banner placeholder reserved for future advertising

## Build APK

Push to `main` or open **Actions → Build Android APK → Run workflow**. The workflow builds `app-debug.apk` and uploads it as the `Video-Downloader-debug` artifact. Open the successful workflow run and download the artifact ZIP to retrieve the APK.

## Limitations

- Downloads work with direct HTTP/HTTPS file or media URLs that the source permits.
- A browser page is not the same as a direct media URL; many streaming services use segmented streams, authentication, or DRM and will not download through the direct-link downloader.
- YouTube or other protected media extraction is not implemented. The app does not bypass DRM, sign-in, paywalls, or platform restrictions.
- MP3 conversion, quality selection for streaming sources, persistent/resumable downloads after app restart, and real ad network integration are not implemented.
- Downloaded files are stored in the app-specific Downloads directory. Share/open actions can pass files to other apps.

Only download content you own or are authorized to save.
