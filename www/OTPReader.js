var exec = require('cordova/exec');

/**
 * OTP Reader Plugin for Cordova
 * Provides automatic OTP reading functionality using Android SMS User Consent API
 */
var OTPReader = {
    
    /**
     * Start listening for OTP SMS messages
     * @param {string} senderPhoneNumber - Optional sender phone number to filter messages
     * @param {function} successCallback - Success callback function
     * @param {function} errorCallback - Error callback function
     */
    startListening: function(senderPhoneNumber, successCallback, errorCallback) {
        if (typeof senderPhoneNumber === 'function') {
            // If first parameter is a function, it means no phone number was provided
            errorCallback = successCallback;
            successCallback = senderPhoneNumber;
            senderPhoneNumber = null;
        }
        
        exec(successCallback, errorCallback, 'OTPReader', 'startListening', [senderPhoneNumber]);
    },
    
    /**
     * Stop listening for OTP SMS messages
     * @param {function} successCallback - Success callback function
     * @param {function} errorCallback - Error callback function
     */
    stopListening: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'OTPReader', 'stopListening', []);
    },
    
    /**
     * Get the user's phone number hint (requires user interaction)
     * @param {function} successCallback - Success callback function
     * @param {function} errorCallback - Error callback function
     */
    getPhoneNumberHint: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'OTPReader', 'getPhoneNumberHint', []);
    },
    
    /**
     * Extract OTP from SMS message text
     * @param {string} message - SMS message text
     * @param {number} otpLength - Expected OTP length (default: 6)
     * @returns {string|null} - Extracted OTP or null if not found
     */
    extractOTP: function(message, otpLength) {
        otpLength = otpLength || 6;
        
        // Common OTP patterns
        var patterns = [
            // Exact length digits
            new RegExp('\\b\\d{' + otpLength + '}\\b'),
            // OTP: followed by digits
            new RegExp('(?:otp|code|verification|pin)\\s*:?\\s*(\\d{' + otpLength + '})', 'i'),
            // Digits within word boundaries
            new RegExp('\\b(\\d{' + otpLength + '})\\b'),
            // Any sequence of digits of specified length
            new RegExp('(\\d{' + otpLength + '})')
        ];
        
        for (var i = 0; i < patterns.length; i++) {
            var match = message.match(patterns[i]);
            if (match) {
                return match[1] || match[0];
            }
        }
        
        return null;
    }
};

module.exports = OTPReader;
