# SMS Detection Troubleshooting Guide

## Common Issues and Solutions

### 1. SMS Format Requirements

The SMS User Consent API only detects SMS messages that contain:
- **4-10 character alphanumeric string** with at least one number
- Must be received **AFTER** starting the SMS User Consent listener
- The SMS should contain patterns like: `123456`, `ABC123`, `1234AB`

**Working SMS Examples:**
```
Your verification code is 123456
OTP: 654321
Code 789012 expires in 10 minutes
Use code ABC123 to verify
```

**Non-Working SMS Examples:**
```
Your code is 12345678901 (too long)
Use code ABC to verify (no numbers)
Verification: 123 (too short)
```

### 2. Timing Issues

**Critical:** The SMS User Consent API must be started **BEFORE** the SMS arrives.

**Correct Flow:**
1. User requests OTP
2. App starts listening (`cordova.plugins.OTPReader.startListening()`)
3. App sends request to server to send SMS
4. SMS arrives and is detected

**Incorrect Flow:**
1. User requests OTP
2. Server sends SMS immediately
3. App starts listening (too late - SMS already arrived)

### 3. Testing with Real SMS

For testing, use a real SMS service or send SMS manually:

```javascript
// Start listening first
cordova.plugins.OTPReader.startListening(null, 
  function(result) {
    console.log('SMS detected:', result);
  }, 
  function(error) {
    console.log('Error:', error);
  }
);

// Then send SMS to your device with text like:
// "Your verification code is 123456"
```

### 4. Debug Logging

Add this to check if the plugin is working:

```javascript
// Check if plugin is available
if (cordova.plugins && cordova.plugins.OTPReader) {
  console.log('OTP Reader plugin is available');
  
  // Test basic functionality
  cordova.plugins.OTPReader.startListening(null,
    function(result) {
      console.log('✅ SMS Result:', JSON.stringify(result, null, 2));
      
      if (result.success && result.message) {
        console.log('📱 SMS Message:', result.message);
        const otp = cordova.plugins.OTPReader.extractOTP(result.message, 6);
        console.log('🔢 Extracted OTP:', otp);
      } else if (result.timeout) {
        console.log('⏰ SMS listening timeout');
      } else if (result.userCancelled) {
        console.log('❌ User cancelled SMS access');
      }
    },
    function(error) {
      console.error('❌ OTP Error:', error);
    }
  );
} else {
  console.error('OTP Reader plugin not found');
}
```

### 5. Check Android Logs

Use `adb logcat` to see detailed logs:

```bash
# Filter for OTP-related logs
adb logcat | grep -E "OTPReader|SMSBroadcastReceiver|SmsRetriever"

# Look for these log messages:
# "SMS User Consent started successfully"
# "Broadcast received with action"
# "SMS_RETRIEVED_ACTION received with extras"
# "SMS Retriever Status Code"
```

### 6. Permission Check

Ensure these permissions are granted:
- `android.permission.RECEIVE_SMS` (automatically added by plugin)
- `android.permission.READ_PHONE_STATE` (automatically added by plugin)

### 7. Google Play Services

Ensure Google Play Services is:
- Installed and updated
- Version 19.8.31 or higher for proper SMS User Consent support

### 8. Test on Physical Device

SMS User Consent API works better on physical devices with active SIM cards rather than emulators.

## Quick Test Steps

1. **Clean build:**
   ```bash
   meteor reset
   meteor run android-device
   ```

2. **Start listening in app:**
   ```javascript
   cordova.plugins.OTPReader.startListening(null, callback, errorCallback);
   ```

3. **Send test SMS from another phone:**
   ```
   Text: "Your verification code is 123456"
   ```

4. **Check if consent dialog appears**

5. **Grant permission and check if OTP is extracted**

## Expected Log Output

```
I/OTPReader: SMS User Consent started successfully
I/OTPReader: Listening for SMS from: any sender
I/SMSBroadcastReceiver: Broadcast received with action: com.google.android.gms.auth.api.phone.SMS_RETRIEVED
I/SMSBroadcastReceiver: SMS_RETRIEVED_ACTION received with extras
I/SMSBroadcastReceiver: SMS Retriever Status Code: 0
I/SMSBroadcastReceiver: SMS retrieval successful
```

If you don't see these logs, the issue is likely in the setup or SMS format.
