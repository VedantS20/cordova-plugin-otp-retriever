// OTP Reader Debug and Test Script
// Add this to your browser console or app for debugging

window.OTPReaderDebug = {
    
    // Test basic plugin availability
    checkPlugin: function() {
        console.log('=== OTP Reader Plugin Check ===');
        
        if (window.cordova && cordova.plugins && cordova.plugins.OTPReader) {
            console.log('✅ OTP Reader plugin is available');
            return true;
        } else {
            console.log('❌ OTP Reader plugin not found');
            console.log('Available plugins:', Object.keys(cordova.plugins || {}));
            return false;
        }
    },
    
    // Get comprehensive debug information
    getDebugInfo: function() {
        console.log('=== Getting Debug Information ===');
        
        if (!this.checkPlugin()) return;
        
        cordova.plugins.OTPReader.getDebugInfo(
            function(debugInfo) {
                console.log('✅ Debug Info:', debugInfo);
                
                // Analyze the debug info
                if (!debugInfo.playServicesAvailable) {
                    console.log('⚠️  Google Play Services not available or outdated');
                }
                
                if (!debugInfo.permissions['android.permission.RECEIVE_SMS']) {
                    console.log('⚠️  RECEIVE_SMS permission not granted');
                }
                
                if (!debugInfo.permissions['android.permission.READ_PHONE_STATE']) {
                    console.log('⚠️  READ_PHONE_STATE permission not granted');
                }
                
                console.log('📱 Android Version:', debugInfo.androidRelease);
                console.log('🔢 SDK Level:', debugInfo.androidVersion);
                console.log('👂 Currently Listening:', debugInfo.isListening);
            },
            function(error) {
                console.error('❌ Debug Info Error:', error);
            }
        );
    },
    
    // Start listening with detailed logging
    startListening: function(senderPhone) {
        console.log('=== Starting OTP Listener ===');
        console.log('Sender filter:', senderPhone || 'Any sender');
        
        if (!this.checkPlugin()) return;
        
        cordova.plugins.OTPReader.startListening(
            senderPhone || null,
            function(result) {
                console.log('📱 OTP Reader Result:', result);
                
                if (result.success && result.message) {
                    console.log('✅ SMS Detected!');
                    console.log('📝 Message:', result.message);
                    
                    // Try different OTP lengths
                    for (let length = 4; length <= 8; length++) {
                        const otp = cordova.plugins.OTPReader.extractOTP(result.message, length);
                        if (otp) {
                            console.log(`🔢 OTP (${length} digits):`, otp);
                        }
                    }
                } else if (result.timeout) {
                    console.log('⏰ SMS listening timeout (5 minutes elapsed)');
                } else if (result.userCancelled) {
                    console.log('❌ User cancelled SMS permission');
                } else {
                    console.log('⚠️  Unknown result:', result);
                }
            },
            function(error) {
                console.error('❌ OTP Listener Error:', error);
            }
        );
    },
    
    // Stop listening
    stopListening: function() {
        console.log('=== Stopping OTP Listener ===');
        
        if (!this.checkPlugin()) return;
        
        cordova.plugins.OTPReader.stopListening(
            function() {
                console.log('✅ Stopped listening for SMS');
            },
            function(error) {
                console.error('❌ Stop error:', error);
            }
        );
    },
    
    // Test OTP extraction with various patterns
    testOTPExtraction: function() {
        console.log('=== Testing OTP Extraction ===');
        
        if (!this.checkPlugin()) return;
        
        const testMessages = [
            "Your verification code is 123456",
            "OTP: 654321",
            "Code 789012 expires in 10 minutes",
            "Use 345678 to verify your account",
            "Verification: 901234",
            "234567 is your login code",
            "Your code ABC123 for verification",
            "PIN: 567890",
            "Access code: 112233",
            "Confirm with 445566"
        ];
        
        testMessages.forEach(message => {
            const otp = cordova.plugins.OTPReader.extractOTP(message, 6);
            console.log(`Message: "${message}"`);
            console.log(`OTP: ${otp || 'Not found'}`);
            console.log('---');
        });
    },
    
    // Complete test sequence
    runFullTest: function() {
        console.log('🚀 Starting Full OTP Reader Test');
        console.log('=====================================');
        
        this.checkPlugin();
        setTimeout(() => this.getDebugInfo(), 1000);
        setTimeout(() => this.testOTPExtraction(), 2000);
        setTimeout(() => {
            console.log('📱 Starting SMS listener...');
            console.log('Now send an SMS with format: "Your verification code is 123456"');
            this.startListening();
        }, 3000);
    },
    
    // Instructions for manual testing
    showInstructions: function() {
        console.log(`
🔧 OTP Reader Manual Testing Instructions:

1. Run: OTPReaderDebug.runFullTest()
2. Check debug info for any issues
3. When listener starts, send SMS from another phone:
   
   📱 SMS Text Examples:
   "Your verification code is 123456"
   "OTP: 654321"
   "Code 789012 expires in 10 minutes"
   
4. Look for consent dialog on device
5. Grant permission when prompted
6. Check console for extracted OTP

🐛 Common Issues:
- SMS must arrive AFTER starting listener
- SMS must contain 4-10 character code with at least 1 number
- Google Play Services must be available and updated
- Test on real device with SIM card

📋 Available Commands:
- OTPReaderDebug.checkPlugin()
- OTPReaderDebug.getDebugInfo() 
- OTPReaderDebug.startListening()
- OTPReaderDebug.stopListening()
- OTPReaderDebug.testOTPExtraction()
- OTPReaderDebug.runFullTest()
        `);
    }
};

// Auto-run instructions
if (typeof cordova !== 'undefined') {
    console.log('🔧 OTP Reader Debug Tools Loaded!');
    console.log('Run: OTPReaderDebug.showInstructions() for help');
    console.log('Run: OTPReaderDebug.runFullTest() to start testing');
} else {
    console.log('⚠️  Cordova not available - run this in your mobile app');
}
