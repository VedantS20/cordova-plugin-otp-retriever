# Cordova OTP Reader Plugin

A Cordova plugin for automatic OTP (One-Time Password) reading using Android's SMS User Consent API. This plugin allows your Meteor app to automatically read and fill OTP codes from SMS messages with user consent.

## Features

- ✅ Uses latest Android SMS User Consent API (2024)
- ✅ User consent-based SMS reading (privacy-friendly)
- ✅ Automatic OTP extraction from SMS messages
- ✅ Phone number hint picker
- ✅ Works with any SMS format
- ✅ No special SMS format required
- ✅ Meteor app integration ready

## Requirements

- Cordova >= 7.0.0
- cordova-android >= 8.0.0
- Android API level 16+ (Android 4.1+)
- Google Play Services

## Installation

### 1. Install the plugin

```bash
# For Cordova apps
cordova plugin add cordova-plugin-otp-reader

# For Meteor mobile apps
meteor add cordova:cordova-plugin-otp-reader@file://path/to/cordova-plugin-otp-reader
```

### 2. For Meteor apps, add to mobile-config.js:

```javascript
App.addCordovaPlugin('cordova-plugin-otp-reader', {
  version: '1.0.0',
  source: 'https://github.com/your-username/cordova-plugin-otp-reader.git'
});
```

## Usage

### Basic Usage

```javascript
// Start listening for OTP SMS messages
cordova.plugins.OTPReader.startListening(
  null, // sender phone number (optional)
  function(result) {
    if (result.success) {
      console.log('SMS Message:', result.message);
      
      // Extract OTP from message
      var otp = cordova.plugins.OTPReader.extractOTP(result.message, 6);
      console.log('Extracted OTP:', otp);
      
      // Auto-fill the OTP in your form
      document.getElementById('otpInput').value = otp;
    } else if (result.userCancelled) {
      console.log('User cancelled SMS access');
    } else if (result.timeout) {
      console.log('SMS listening timeout');
    }
  },
  function(error) {
    console.error('Error:', error);
  }
);
```

### Meteor Integration Example

```javascript
// In your Meteor client code
Template.otpVerification.events({
  'click #verifyOTP': function() {
    if (Meteor.isCordova) {
      // Start listening for OTP
      cordova.plugins.OTPReader.startListening(
        null, // No specific sender
        function(result) {
          if (result.success) {
            var otp = cordova.plugins.OTPReader.extractOTP(result.message, 6);
            if (otp) {
              // Update reactive variable
              Template.instance().otpCode.set(otp);
              
              // Auto-submit if needed
              Meteor.call('verifyOTP', otp, function(err, res) {
                if (err) {
                  console.error('OTP verification failed:', err);
                } else {
                  console.log('OTP verified successfully');
                }
              });
            }
          }
        },
        function(error) {
          console.error('OTP reading error:', error);
        }
      );
    }
  }
});

Template.otpVerification.helpers({
  otpCode: function() {
    return Template.instance().otpCode.get();
  }
});

Template.otpVerification.onCreated(function() {
  this.otpCode = new ReactiveVar('');
});
```

### Advanced Usage with Sender Filtering

```javascript
// Listen for SMS from specific sender
var senderPhoneNumber = '+1234567890';

cordova.plugins.OTPReader.startListening(
  senderPhoneNumber,
  function(result) {
    if (result.success) {
      var otp = cordova.plugins.OTPReader.extractOTP(result.message, 4); // 4-digit OTP
      console.log('OTP from verified sender:', otp);
    }
  },
  function(error) {
    console.error('Error:', error);
  }
);
```

### Get Phone Number Hint

```javascript
// Get user's phone number (requires user interaction)
cordova.plugins.OTPReader.getPhoneNumberHint(
  function(phoneNumber) {
    console.log('User phone number:', phoneNumber);
    document.getElementById('phoneInput').value = phoneNumber;
  },
  function(error) {
    console.error('Error getting phone number:', error);
  }
);
```

### Stop Listening

```javascript
// Stop listening for SMS messages
cordova.plugins.OTPReader.stopListening(
  function() {
    console.log('Stopped listening for SMS');
  },
  function(error) {
    console.error('Error stopping listener:', error);
  }
);
```

## API Reference

### Methods

#### `startListening(senderPhoneNumber, successCallback, errorCallback)`

Starts listening for SMS messages containing OTP.

**Parameters:**
- `senderPhoneNumber` (string, optional): Phone number to filter messages from
- `successCallback` (function): Called when SMS is received or events occur
- `errorCallback` (function): Called when an error occurs

**Success Callback Response:**
```javascript
{
  success: true,          // boolean - true if SMS was successfully read
  message: "Your OTP...", // string - full SMS message text
  userCancelled: false,   // boolean - true if user denied permission
  timeout: false          // boolean - true if listening timeout occurred
}
```

#### `stopListening(successCallback, errorCallback)`

Stops listening for SMS messages.

#### `getPhoneNumberHint(successCallback, errorCallback)`

Shows a picker to get the user's phone number.

#### `extractOTP(message, otpLength)`

Extracts OTP from SMS message text (client-side utility).

**Parameters:**
- `message` (string): SMS message text
- `otpLength` (number, optional): Expected OTP length (default: 6)

**Returns:** String containing the OTP or null if not found

## Meteor Mobile App Setup

### 1. Add to your Meteor project

```bash
# Add mobile platforms
meteor add-platform android

# Add the plugin via mobile-config.js
# See installation section above
```

### 2. Configure mobile-config.js

```javascript
App.info({
  id: 'com.yourcompany.yourapp',
  name: 'Your App Name',
  description: 'Your app description',
  author: 'Your Company',
  email: 'contact@yourcompany.com',
  website: 'http://yourcompany.com'
});

// Add the OTP Reader plugin
App.addCordovaPlugin('cordova-plugin-otp-reader', {
  version: '1.0.0'
});

// Configure Android permissions (automatically handled by plugin)
App.setPreference('android-targetSdkVersion', '33');
App.setPreference('android-minSdkVersion', '21');
```

### 3. Build and test

```bash
# Build for Android
meteor build ../output --mobile-settings settings.json

# Or run on device
meteor run android-device --mobile-settings settings.json
```

## How It Works

1. **SMS User Consent API**: Uses Google's official SMS User Consent API for privacy-compliant SMS reading
2. **User Permission**: Prompts user for permission to read a single SMS message
3. **Automatic Detection**: Detects SMS messages containing 4-10 character alphanumeric codes with at least one number
4. **Message Filtering**: Optionally filters messages by sender phone number
5. **OTP Extraction**: Provides utility functions to extract OTP from various SMS formats

## Privacy & Security

- ✅ **User Consent Required**: User must explicitly grant permission for each SMS
- ✅ **Single Message Access**: Only reads one SMS message per permission grant
- ✅ **No Persistent Permissions**: No ongoing SMS reading permissions required
- ✅ **Sender Filtering**: Can limit to specific sender phone numbers
- ✅ **Timeout Protection**: Automatically stops listening after 5 minutes

## Common SMS Formats Supported

The plugin works with various OTP SMS formats:

```
"Your verification code is 123456"
"OTP: 123456"
"Code 123456 expires in 10 minutes"
"Your OTP is 123456. Do not share."
"123456 is your verification code"
```

## Troubleshooting

### Plugin not working:
- Ensure Google Play Services is installed and updated
- Check that app has proper permissions in AndroidManifest.xml
- Verify Android API level is 16 or higher

### OTP not detected:
- Check if SMS contains 4-10 character alphanumeric code with at least one number
- Verify sender phone number if filtering is enabled
- Try different OTP extraction patterns

### User consent not showing:
- Ensure SMS User Consent was started before SMS arrival
- Check that timeout hasn't occurred (5 minutes max)
- Verify device has active internet connection

## Example Project Structure

```
meteor-app/
├── mobile-config.js
├── client/
│   ├── templates/
│   │   ├── otp-verification.html
│   │   └── otp-verification.js
│   └── lib/
│       └── otp-handler.js
├── server/
│   └── methods.js
└── packages/
    └── cordova-plugin-otp-reader/
```

## License

MIT License - see LICENSE file for details.

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Support

For issues and questions:
- Create an issue on GitHub
- Check existing issues for solutions
- Review Android SMS User Consent API documentation

## Changelog

### 1.0.0
- Initial release
- SMS User Consent API integration
- Phone number hint picker
- OTP extraction utilities
- Meteor app integration examples
