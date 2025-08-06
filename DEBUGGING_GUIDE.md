# OTP Reader Debug Guide

## Common Issues and Solutions

### 1. **TIMING ISSUE** (Most Common)
**Problem**: SMS sent at the same time as starting listener won't be detected.

**Solution**: 
1. Start listening first
2. Wait for "Started listening" response 
3. THEN send the SMS

**Test**: Use the debug_test.html page to verify timing.

### 2. **Sender Phone Number Format**
**Problem**: If you specify a sender number, it must match exactly.

**Examples of what might work**:
- `null` - Listen for any sender (recommended for testing)
- `"+1234567890"` - For full international format
- `"1234567890"` - For national format

**Test Code**:
```javascript
// Test with any sender first
cordova.plugins.OTPReader.startListening(null, successCallback, errorCallback);

// Then test with specific sender
cordova.plugins.OTPReader.startListening("XXXXXX-S", successCallback, errorCallback);
```

### 3. **SMS User Consent Workflow**
The SMS User Consent API works in these steps:
1. Start listening → Returns "listening: true"
2. SMS arrives → Broadcast receiver triggers
3. User consent dialog appears → User must approve
4. SMS content delivered → You get the message

**Check**: Look for "SMS BROADCAST RECEIVER TRIGGERED" in logs when SMS arrives.

### 4. **Android Version Issues**
- **Android 13+**: Receiver export requirements (handled in code)
- **Android 6+**: Runtime permissions required (handled in code)

### 5. **Google Play Services**
**Check**: Use getDebugInfo() to verify Play Services status.

## Debugging Steps

### Step 1: Check Plugin Status
```javascript
cordova.plugins.OTPReader.getDebugInfo(
    function(result) {
        console.log('Debug Info:', result);
        // Check: isListening, permissions, playServicesAvailable
    },
    function(error) {
        console.log('Debug Error:', error);
    }
);
```

### Step 2: Test Basic Functionality
```javascript
// Start listening for any sender
cordova.plugins.OTPReader.startListening(
    null, // Any sender
    function(result) {
        console.log('Success:', result);
        if (result.listening) {
            console.log('✅ NOW SEND YOUR SMS!');
        } else if (result.message) {
            console.log('📱 SMS Received:', result.message);
        }
    },
    function(error) {
        console.log('❌ Error:', error);
    }
);
```

### Step 3: Check Android Logs
Connect device and run:
```bash
adb logcat | grep -E "(OTPReader|SMSBroadcastReceiver)"
```

Look for these key messages:
- `SMS USER CONSENT STARTED SUCCESSFULLY`
- `SMS BROADCAST RECEIVER TRIGGERED` (when SMS arrives)
- `SMS RETRIEVAL SUCCESSFUL` (when consent dialog should appear)

### Step 4: Test Different Scenarios

#### Scenario A: Any Sender
```javascript
cordova.plugins.OTPReader.startListening(null, callback, errorCallback);
```

#### Scenario B: Specific Sender
```javascript
cordova.plugins.OTPReader.startListening("XXXXXX-S", callback, errorCallback);
```

#### Scenario C: Phone Number Sender
```javascript
cordova.plugins.OTPReader.startListening("+1234567890", callback, errorCallback);
```

## Expected Log Flow

### When Working Correctly:
```
[OTPReader] === STARTING SMS USER CONSENT ===
[OTPReader] Sender phone number parameter: null (any sender)
[OTPReader] === SMS USER CONSENT STARTED SUCCESSFULLY ===
[OTPReader] ⚠️  CRITICAL: Send your OTP SMS NOW!
// ... SMS is sent ...
[SMSBroadcastReceiver] === SMS BROADCAST RECEIVER TRIGGERED ===
[SMSBroadcastReceiver] SMS_RETRIEVED_ACTION received with extras
[SMSBroadcastReceiver] SMS Retriever Status Code: 0
[SMSBroadcastReceiver] === SMS RETRIEVAL SUCCESSFUL ===
[SMSBroadcastReceiver] Consent intent found, showing user consent dialog
// ... User approves dialog ...
[OTPReader] SMS received: "Your OTP is 123456"
```

### When NOT Working:
1. **No broadcast received**: SMS wasn't detected by Google Play Services
2. **Timeout**: SMS took too long or wasn't detected
3. **User denied**: User rejected the consent dialog

## Quick Fix Attempts

### Try 1: Basic Test
1. Open debug_test.html in your app
2. Click "Start Listening (Any Sender)"
3. Wait for "Started listening" message
4. Send SMS from any phone
5. Check if broadcast receiver triggers

### Try 2: Remove Sender Filter
Change your client code to use `null` instead of specific sender:
```javascript
cordova.plugins.OTPReader.startListening(null, callback, errorCallback);
```

### Try 3: Check Permissions
Verify SMS permissions are granted:
```javascript
cordova.plugins.OTPReader.getDebugInfo(function(result) {
    console.log('Permissions:', result.permissions);
});
```

### Try 4: Restart App
Sometimes the broadcast receiver needs a fresh start after permission changes.

