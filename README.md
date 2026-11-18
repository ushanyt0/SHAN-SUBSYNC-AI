# SHAN SUBSYNC AI

**Intelligent Sinhala Subtitle Synchronization**  
Powered by Shan Codex.

An Android-first video player and subtitle synchronization project. The planned workflow analyzes a video, aligns an existing Sinhala SRT subtitle to dialogue timing, previews the result, and exports a corrected SRT.

## Architecture

- **Android:** Kotlin, Jetpack Compose, Media3 ExoPlayer
- **Subtitle core:** SRT parser, timeline analysis and drift correction
- **Hybrid AI:** cloud speech/alignment service when configured; optional on-device processing where device resources permit
- **Export:** UTF-8 SRT compatible with VLC and other players

## Important design note

Sinhala translations do not share the same words as English dialogue. The alignment engine must therefore combine dialogue timing, subtitle order and semantic matching; simple speech-to-text word matching is not sufficient.

## Project status

Early scaffold. Video playback, subtitle import, synchronization engine, cloud service integration and offline model support are not implemented yet.

## Build

Open the project in Android Studio and use the included GitHub Actions workflow to build a debug APK. A production release will require signing configuration.
