// Example Meteor mobile-config.js for OTP Reader plugin

App.info({
  id: 'com.fasal.otpapp',
  name: 'OTP Reader Demo',
  description: 'Demo app for automatic OTP reading',
  author: 'Fasal Team',
  email: 'tech@fasal.co',
  website: 'https://fasal.co'
});

// Set app preferences
App.setPreference('android-targetSdkVersion', '33');
App.setPreference('android-minSdkVersion', '21');
App.setPreference('android-compileSdkVersion', '33');

// Add the OTP Reader plugin
App.addCordovaPlugin('cordova-plugin-otp-reader', {
  version: '1.0.0'
});

// Configure app icons (optional)
App.icons({
  'android_mdpi': 'resources/icons/icon-48x48.png',
  'android_hdpi': 'resources/icons/icon-72x72.png',
  'android_xhdpi': 'resources/icons/icon-96x96.png',
  'android_xxhdpi': 'resources/icons/icon-144x144.png',
  'android_xxxhdpi': 'resources/icons/icon-192x192.png'
});

// Configure launch screens (optional)
App.launchScreens({
  'android_mdpi_portrait': 'resources/splash/splash-320x470.png',
  'android_hdpi_portrait': 'resources/splash/splash-480x640.png',
  'android_xhdpi_portrait': 'resources/splash/splash-720x960.png',
  'android_xxhdpi_portrait': 'resources/splash/splash-960x1280.png',
  'android_xxxhdpi_portrait': 'resources/splash/splash-1280x1920.png'
});

// Additional Android permissions (handled automatically by plugin)
// These are just for reference - the plugin adds them automatically
/*
App.appendToConfig(`
  <platform name="android">
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
  </platform>
`);
*/
