# Sonic Switcher

Sonic Switcher is a simple audio converter for Android, designed to convert various audio file formats into high-quality MP3s. Once converted, you can save the files to your device or easily share them using Android's native sharing capabilities.

## Features

*   Select any audio file from your device's storage.
*   Convert audio files to MP3 format.
*   View the progress of the conversion in real-time.
*   Save the converted MP3 to your device.
*   Share the converted file with other apps.

## Project Architecture and Structure

This project is intended to follow modern Android development practices and maintains a clean, modular architecture.

*   **Modular Design**: The project is split into two main modules:
    *   `:app`: This is the main application module, containing the user interface (UI) and all application-level logic. It is responsible for handling user interactions, permissions, and coordinating with the `:converter` module.
    *   `:converter`: This is a library module that contains the core audio conversion logic. It uses `FFmpegKit` to handle the heavy lifting of audio transcoding. This module is self-contained and has no dependencies on the `:app` module, making it reusable.

*   **UI**: The user interface is built entirely with **Jetpack Compose**, Google's modern toolkit for building native Android UI.

*   **Dependency Injection**: **Hilt** is used for dependency injection, which helps in managing dependencies and creating a more scalable and testable codebase.

*   **Asynchronous Operations**: **Kotlin Coroutines** are used to manage background threads, ensuring that long-running operations like file conversions don't block the main thread, providing a smooth user experience.

## Building the Project

To build the project, you will need Android Studio.

1.  Clone the repository: `git clone https://github.com/your-username/sonic-switcher.git`
2.  Open the project in Android Studio.
3.  Let Gradle sync and download the required dependencies.
4.  Build and run the app on an Android device or emulator.

## Contributing

This is an open-source project, and contributions are welcome! If you'd like to contribute, please feel free to fork the repository and submit a pull request. If you find any issues or have suggestions for improvements, please open an issue.
