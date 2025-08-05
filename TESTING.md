# Testing Guide for Cordova OTP Reader Plugin

This guide provides comprehensive instructions for testing the OTP Reader plugin in your Meteor mobile app.

## Pre-requisites

1. **Android Device or Emulator** with Google Play Services
2. **Physical Device Recommended** (SMS functionality works better on real devices)
3. **Active SIM Card** (for receiving actual SMS messages)
4. **Meteor Development Environment** set up

## Setup for Testing

### 1. Create Test Meteor App

```bash
# Create new Meteor app
meteor create otp-test-app
cd otp-test-app

# Add mobile platform
meteor add-platform android

# Install the plugin
meteor add cordova:cordova-plugin-otp-reader@file://path/to/cordova-plugin-otp-reader
```

### 2. Configure mobile-config.js

```javascript
// mobile-config.js
App.info({
  id: 'com.test.otpreader',
  name: 'OTP Test App'
});

App.addCordovaPlugin('cordova-plugin-otp-reader');
```

### 3. Add Test Templates

Copy the example files from the `examples/` directory into your Meteor app:

```bash
# Copy example files to your Meteor app
cp examples/otp-templates.html client/
cp examples/otp-client.js client/
cp examples/otp-server.js server/
cp examples/otp-styles.css client/
```

## Test Cases

### Test Case 1: Basic OTP Detection

**Objective**: Verify that the plugin can detect and extract OTP from SMS messages.

**Steps**:
1. Build and run the app on device: `meteor run android-device`
2. Navigate to OTP verification screen
3. Tap "Auto-fill from SMS" button
4. Grant SMS permissions when prompted
5. Send a test SMS to the device: "Your verification code is 123456"
6. Verify that the OTP "123456" is automatically filled

**Expected Result**: OTP should be extracted and filled automatically.

### Test Case 2: Different OTP Formats

**Objective**: Test various SMS formats to ensure robust OTP extraction.

**Test SMS Messages**:
```
"Your OTP is 654321"
"OTP: 789012"
"Code 345678 expires in 10 minutes"
"Use 901234 to verify your account"
"Verification code: 567890"
"234567 is your login code"
```

**Expected Result**: All OTPs should be correctly extracted.

### Test Case 3: Sender Filtering

**Objective**: Test sender-specific OTP reading.

**Steps**:
1. Modify code to specify sender: `startListening('+1234567890', callback, errorCallback)`
2. Send SMS from the specified number
3. Send SMS from a different number
4. Verify only SMS from specified sender triggers the callback

**Expected Result**: Only SMS from specified sender should be processed.

### Test Case 4: User Consent Flow

**Objective**: Verify the user consent dialog appears and works correctly.

**Steps**:
1. Start listening for OTP
2. Send test SMS
3. Verify consent dialog appears
4. Test both "Allow" and "Deny" options
5. Check appropriate callbacks are triggered

**Expected Result**: 
- "Allow": OTP should be extracted
- "Deny": Error callback should be triggered with `userCancelled: true`

### Test Case 5: Timeout Handling

**Objective**: Test timeout behavior when no SMS is received.

**Steps**:
1. Start listening for OTP
2. Wait for 5+ minutes without sending SMS
3. Verify timeout callback is triggered

**Expected Result**: Timeout callback should be triggered after 5 minutes.

### Test Case 6: Phone Number Access

**Objective**: Test device phone number access functionality.

**Steps**:
1. Call `getPhoneNumber()` method
2. Check if phone number is returned
3. Verify proper error handling if not available

**Expected Result**: Device phone number should be returned if available and permitted.

### Test Case 7: Multiple OTP Sessions

**Objective**: Test handling of multiple OTP requests.

**Steps**:
1. Start listening for OTP
2. Start another OTP session
3. Verify proper error handling
4. Stop first session and start new one
5. Verify new session works correctly

**Expected Result**: Should handle multiple sessions gracefully.

### Test Case 8: App Lifecycle

**Objective**: Test plugin behavior during app lifecycle events.

**Steps**:
1. Start OTP listening
2. Put app in background
3. Send SMS while app is in background
4. Bring app to foreground
5. Verify OTP is still detected

**Expected Result**: OTP should be detected even when app is backgrounded.

## Manual Testing Script

Create this test helper in your Meteor app:

```javascript
// client/test-helper.js
if (Meteor.isDevelopment) {
  window.testOTPReader = {
    
    // Test basic OTP reading
    testBasicOTP: function() {
      console.log('Starting basic OTP test...');
      
      cordova.plugins.OTPReader.startListening(
        null,
        function(result) {
          console.log('✅ OTP Result:', result);
          if (result.success) {
            const otp = cordova.plugins.OTPReader.extractOTP(result.message, 6);
            console.log('✅ Extracted OTP:', otp);
          }
        },
        function(error) {
          console.error('❌ OTP Error:', error);
        }
      );
    },
    
    // Test phone number access
    testPhoneNumber: function() {
      console.log('Testing phone number access...');
      
      cordova.plugins.OTPReader.getPhoneNumber(
        function(phoneNumber) {
          console.log('✅ Device phone number:', phoneNumber);
        },
        function(error) {
          console.error('❌ Phone number error:', error);
        }
      );
    },
    
    // Test OTP extraction patterns
    testOTPExtraction: function() {
      const testMessages = [
        "Your verification code is 123456",
        "OTP: 654321",
        "Code 789012 expires soon",
        "Use 345678 to verify",
        "234567 is your code"
      ];
      
      testMessages.forEach(message => {
        const otp = cordova.plugins.OTPReader.extractOTP(message, 6);
        console.log(`Message: "${message}" -> OTP: ${otp}`);
      });
    },
    
    // Stop listening
    stopListening: function() {
      cordova.plugins.OTPReader.stopListening(
        function() {
          console.log('✅ Stopped listening');
        },
        function(error) {
          console.error('❌ Stop error:', error);
        }
      );
    }
  };
  
  // Make it available globally
  console.log('OTP Reader test functions available:');
  console.log('- testOTPReader.testBasicOTP()');
  console.log('- testOTPReader.testPhoneNumber()');
  console.log('- testOTPReader.testOTPExtraction()');
  console.log('- testOTPReader.stopListening()');
}
```

## Testing with Real SMS Services

### Using Twilio for Testing

```javascript
// Send test SMS using Twilio (server-side)
const twilio = require('twilio')(accountSid, authToken);

// Test different OTP formats
const testMessages = [
  "Your verification code is 123456",
  "OTP: 654321",  
  "Code 789012 expires in 5 minutes"
];

testMessages.forEach((message, index) => {
  setTimeout(() => {
    twilio.messages.create({
      body: message,
      from: '+1234567890',
      to: '+your-test-number'
    });
  }, index * 10000); // 10 second intervals
});
```

## Debugging Issues

### Common Issues and Solutions

1. **Plugin not found**
   - Verify plugin is properly installed
   - Check `cordova plugin list`
   - Rebuild app completely

2. **SMS not detected**
   - Check device has Google Play Services
   - Verify SMS contains 4-10 alphanumeric characters with at least one number
   - Check sender number if filtering is enabled

3. **Consent dialog not appearing**
   - Ensure SMS User Consent was started before SMS arrival
   - Check for timeout (5 minutes max)
   - Verify device has internet connection

4. **OTP extraction failing**
   - Test with different OTP patterns
   - Check OTP length parameter
   - Verify SMS message format

### Debug Logging

Enable debug logging in your app:

```javascript
// Add to client code
if (Meteor.isDevelopment) {
  // Log all cordova events
  document.addEventListener('deviceready', function() {
    console.log('Device ready - OTP Reader available:', !!cordova.plugins.OTPReader);
  });
}
```

## Performance Testing

### Memory Usage Test

```javascript
// Monitor memory usage during OTP operations
function testMemoryUsage() {
  const startMemory = performance.memory?.usedJSHeapSize || 0;
  
  // Start OTP listening
  cordova.plugins.OTPReader.startListening(null, 
    function(result) {
      const endMemory = performance.memory?.usedJSHeapSize || 0;
      console.log('Memory used:', (endMemory - startMemory) / 1024, 'KB');
    },
    function(error) {
      console.error('Memory test error:', error);
    }
  );
}
```

### Battery Usage Test

Monitor battery usage during extended OTP listening sessions to ensure the plugin doesn't drain battery excessively.

## Automated Testing

For CI/CD integration, create automated tests:

```javascript
// test/otp-reader.test.js
describe('OTP Reader Plugin', function() {
  
  it('should be available after device ready', function(done) {
    document.addEventListener('deviceready', function() {
      expect(cordova.plugins.OTPReader).toBeDefined();
      done();
    });
  });
  
  it('should extract OTP from various message formats', function() {
    const testCases = [
      { message: "Your code is 123456", expected: "123456" },
      { message: "OTP: 654321", expected: "654321" },
      { message: "Code 789012 expires soon", expected: "789012" }
    ];
    
    testCases.forEach(testCase => {
      const result = cordova.plugins.OTPReader.extractOTP(testCase.message, 6);
      expect(result).toBe(testCase.expected);
    });
  });
  
});
```

## Security Testing

1. **Test with malicious SMS**: Verify plugin doesn't crash with malformed SMS
2. **Test permission handling**: Ensure proper permission requests
3. **Test data validation**: Verify extracted OTP is properly validated

## Production Testing Checklist

Before releasing to production:

- [ ] Test on multiple Android devices
- [ ] Test with different Android versions (API 21+)
- [ ] Test with various OTP formats
- [ ] Test sender filtering functionality
- [ ] Test user consent flow
- [ ] Test timeout handling
- [ ] Test app lifecycle events
- [ ] Test memory usage
- [ ] Test battery usage
- [ ] Verify proper error handling
- [ ] Test with real SMS providers
- [ ] Verify privacy compliance

## Troubleshooting Commands

```bash
# Check Cordova version
cordova --version

# List installed plugins
cordova plugin list

# Check Android platform version
cordova platform list

# Clean and rebuild
meteor reset
meteor run android-device

# Check device logs
adb logcat | grep -i otp
```

This comprehensive testing guide should help you thoroughly validate the OTP Reader plugin functionality in your Meteor app.
