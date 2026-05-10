# Lime

A modern, high-performance Android client for **Kick.com**, built with Jetpack Compose and Material 3. Lime focuses on a clean user experience while providing advanced features like background playback and emote support.

## Features

- **Integrated Stream Player**: Low-latency playback powered by Amazon IVS Player with quality selection support.
- **Background Playback**: Keep listening to your favorite streams even when the app is in the background or the screen is off via a dedicated `PlayerService`.
- **Live Chat**: Real-time chat integration with support for:
  - **7TV Emotes**: Global and channel-specific emotes integrated directly into the chat.
  - **Badges**: Streamer, moderator, and subscriber badges.
- **Dynamic UI**: 
  - **Mini-player Overlay**: Interactive, draggable mini-player that allows you to browse the app while watching.
  - **Edge-to-Edge Design**: Full Material 3 implementation with a refined dark theme and smooth transitions.
  - **Splash Screen**: Branded launch experience.
- **Content Discovery**:
  - **Home Feed**: Recommended live streams and top categories.
  - **Following**: Manage and view live status of followed channels.
  - **Search**: Comprehensive search for channels and games.
- **Safe Authentication**: Secure login flow using `androidx.security.crypto` for credential management.

## Tech Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Multimedia**: Amazon IVS Player
- **Networking**: Retrofit 2, OkHttp 4, Kotlinx Serialization
- **Image/GIF/SVG Loading**: Coil
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow
- **External APIs**: Kick API, 7TV API
- **Tooling**: Ktlint, Detekt for code quality and style enforcement

## Prerequisites

To build and run Lime, you must provide your Kick API credentials in your `local.properties` file:

```properties
kick.client.id=YOUR_CLIENT_ID
kick.client.secret=YOUR_CLIENT_SECRET
```

## Getting Started

1. **Clone the repository**: `git clone https://github.com/yourusername/lime.git`
2. **Setup Credentials**: Add your `kick.client.id` and `kick.client.secret` to `local.properties`.
3. **Android Studio**: Open the project in Android Studio Ladybug or newer.
4. **Target Device**: Run on an Android device or emulator (Minimum API 31 / Android 12).
