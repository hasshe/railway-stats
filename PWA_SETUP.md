# Vaadin PWA Setup

This application is configured as a Progressive Web App (PWA) using Vaadin's AppShell approach.

## Configuration Files

### 1. AppShell.java
Located in `src/main/java/com/hs/railway_stats/AppShell.java`

- Implements `AppShellConfigurator` interface
- Uses `@PWA` annotation to configure PWA settings
- Sets theme color, background color, app name, and description
- Configured for standalone display mode

### 2. manifest.webmanifest
Located in `src/main/frontend/manifest.webmanifest`

- Web app manifest following W3C standard
- Defines app metadata (name, description, icons, theme colors)
- Referenced in the PWA annotation in AppShell.java

## Features

✅ **Offline Support** - Service worker caches resources automatically  
✅ **Install Prompt** - Users can install the app on their device  
✅ **Icon & Theming** - Custom theme colors and app icon  
✅ **Standalone Mode** - Runs as a standalone app without browser UI  

## How It Works

1. When the app builds, Vaadin automatically generates a service worker based on the PWA configuration
2. The manifest.webmanifest tells the browser how to install and display the app
3. The AppShell class provides metadata that Vaadin uses to generate appropriate HTML head tags

## Building & Running

Build the app normally:
```bash
mvn clean package
```

The PWA is automatically configured during the build process. No additional setup needed!

## Testing

1. Open the app in a browser
2. The app should show an "Install" option in the address bar or app menu
3. Click to install the app on your device
4. App will run offline with cached resources

## Customization

To modify PWA settings, edit the `@PWA` annotation in `AppShell.java`:
- Change `name`, `shortName`, `description`
- Update `themeColor` and `backgroundColor`
- Modify `iconPath` to point to your icon file
- Adjust `startPath` if needed
