# TXPacker

TXPacker is an Android application designed to simplify the process of importing resource packs, addons, and worlds into Minecraft Bedrock Edition. With a modern, user-friendly interface and smooth animations, it provides a seamless experience for managing Minecraft content.

![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Min SDK](https://img.shields.io/badge/min%20SDK-24-orange.svg)

## Features

- 🎮 Easy import of `.mcpack`, `.mcaddon`, and `.mcworld` files
- 🌍 Multi-language support with dynamic language switching
- 📱 Adaptive UI for both phones and tablets
- 🎨 Modern Material Design with smooth animations
- ⚙️ Configurable Minecraft package support
- 📝 Comprehensive logging system for troubleshooting
- 🔒 Secure file handling with proper permissions management
- 🌓 Support for system default locale

## Requirements

- Android 7.0 (API level 24) or higher
- Minecraft Bedrock Edition installed
- Storage permissions for file access

## Installation

1. Download the latest APK from the [Releases](https://github.com/lyssadev/TXPacker/releases) page
2. Enable "Install from Unknown Sources" in your Android settings if needed
3. Open the APK and follow the installation prompts

## Usage

1. Launch TXPacker
2. Click the "Import" button to select your Minecraft content file
3. Choose a `.mcpack`, `.mcaddon`, or `.mcworld` file
4. Click "Load" to import the content into Minecraft
5. The app will automatically open Minecraft with your content

## Configuration

### Language Settings
- Access settings through the gear icon
- Choose from available languages or use system default
- Changes take effect immediately

### Minecraft Package
- Configure custom Minecraft package name in settings
- Default: `com.mojang.minecraftpe`
- Useful for different Minecraft versions or beta builds

## Permissions

The app requires the following permissions:
- `READ_EXTERNAL_STORAGE` - For accessing content files
- `MANAGE_EXTERNAL_STORAGE` (Android 11+) - For complete file access

## Building from Source

1. Clone the repository:
```bash
git clone https://github.com/lyssadev/TXPacker.git
```

2. Open the project in Android Studio

3. Sync Gradle and build the project:
```bash
./gradlew build
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Logging

The app includes a comprehensive logging system that helps with troubleshooting. Logs are stored in the app's private directory and can be accessed when logging is enabled in settings.

## Security

- File access is handled securely using Content Providers
- No sensitive data is collected or stored
- All file operations are performed within the app's sandbox
- Proper permission handling for all Android versions

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Material Design Components for Android
- Android Jetpack libraries
- The Minecraft community

## Support

If you encounter any issues or have questions:
1. Check the [Issues](https://github.com/yourusername/TXPacker/issues) page
2. Enable logging in settings for detailed diagnostics
3. Submit a new issue with the log file attached

---

Made with ❤️ for the Minecraft community 
