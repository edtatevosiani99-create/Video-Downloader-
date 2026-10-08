# Video Downloader

An Android app starter with a dark neon interface, embedded browser, direct URL downloads, Android download notifications, and a reserved bottom banner area for future advertising.

## Build APK
Push to `main` or run **Actions → Build Android APK → Run workflow**. The workflow uploads `app-debug.apk` as the `Video-Downloader-debug` artifact.

## Current capabilities
- Neon dark UI and play/download launcher icon
- Embedded browser and URL field
- Direct HTTP(S) file downloads through Android DownloadManager
- Download-complete notifications and files saved to the Downloads folder
- Bottom ad placeholder (not yet connected to an ad provider)

## Important limitations
This starter downloads direct media/file URLs that the source permits. It does not bypass DRM, sign-in, paywalls, or platform restrictions, and it does not extract protected media from YouTube or other services. MP3 conversion, download queue controls (pause/resume), and real ad SDK integration are not implemented yet.
