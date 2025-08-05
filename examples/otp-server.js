// Server-side methods for OTP verification
// Add this to your Meteor server code

Meteor.methods({
  /**
   * Send OTP to phone number
   * @param {Object} params - Parameters object
   * @param {string} params.phoneNumber - Phone number to send OTP to
   * @returns {Object} - Result object with success status
   */
  sendOTP: function(params) {
    check(params, {
      phoneNumber: String
    });
    
    const { phoneNumber } = params;
    
    // Validate phone number format
    if (!isValidPhoneNumber(phoneNumber)) {
      throw new Meteor.Error('invalid-phone', 'Invalid phone number format');
    }
    
    try {
      // Generate 6-digit OTP
      const otp = Math.floor(100000 + Math.random() * 900000).toString();
      
      // Store OTP in database with expiration (5 minutes)
      const expiresAt = new Date(Date.now() + 5 * 60 * 1000);
      
      OTPCollection.upsert(
        { phoneNumber: phoneNumber },
        {
          $set: {
            phoneNumber: phoneNumber,
            otp: otp,
            expiresAt: expiresAt,
            verified: false,
            createdAt: new Date()
          }
        }
      );
      
      // Send SMS using your preferred SMS service
      // Example with Twilio, AWS SNS, or other SMS provider
      const smsResult = sendSMSMessage(phoneNumber, otp);
      
      if (smsResult.success) {
        return {
          success: true,
          message: 'Verification code sent successfully'
        };
      } else {
        throw new Meteor.Error('sms-failed', 'Failed to send SMS');
      }
      
    } catch (error) {
      console.error('Send OTP error:', error);
      throw new Meteor.Error('send-otp-failed', 'Failed to send verification code');
    }
  },
  
  /**
   * Verify OTP
   * @param {Object} params - Parameters object
   * @param {string} params.phoneNumber - Phone number
   * @param {string} params.otp - OTP code to verify
   * @returns {Object} - Result object with success status
   */
  verifyOTP: function(params) {
    check(params, {
      phoneNumber: String,
      otp: String
    });
    
    const { phoneNumber, otp } = params;
    
    try {
      // Find OTP record
      const otpRecord = OTPCollection.findOne({
        phoneNumber: phoneNumber,
        verified: false
      });
      
      if (!otpRecord) {
        throw new Meteor.Error('otp-not-found', 'No verification code found for this phone number');
      }
      
      // Check if OTP has expired
      if (otpRecord.expiresAt < new Date()) {
        // Clean up expired OTP
        OTPCollection.remove({ _id: otpRecord._id });
        throw new Meteor.Error('otp-expired', 'Verification code has expired');
      }
      
      // Check if OTP matches
      if (otpRecord.otp !== otp) {
        throw new Meteor.Error('otp-invalid', 'Invalid verification code');
      }
      
      // Mark OTP as verified
      OTPCollection.update(
        { _id: otpRecord._id },
        {
          $set: {
            verified: true,
            verifiedAt: new Date()
          }
        }
      );
      
      // Update or create user with verified phone number
      const user = Meteor.users.findOne({ 'profile.phoneNumber': phoneNumber });
      
      if (user) {
        Meteor.users.update(user._id, {
          $set: {
            'profile.phoneVerified': true,
            'profile.phoneVerifiedAt': new Date()
          }
        });
      }
      
      return {
        success: true,
        message: 'Phone number verified successfully',
        userId: user ? user._id : null
      };
      
    } catch (error) {
      console.error('Verify OTP error:', error);
      if (error instanceof Meteor.Error) {
        throw error;
      }
      throw new Meteor.Error('verify-otp-failed', 'Failed to verify code');
    }
  }
});

// Helper function to validate phone number
function isValidPhoneNumber(phoneNumber) {
  // Basic phone number validation (adjust regex as needed)
  const phoneRegex = /^\+?[1-9]\d{1,14}$/;
  return phoneRegex.test(phoneNumber.replace(/[\s()-]/g, ''));
}

// SMS sending function - implement with your preferred provider
function sendSMSMessage(phoneNumber, otp) {
  try {
    // Example implementation with Twilio
    if (Meteor.settings.twilio) {
      const twilio = require('twilio')(
        Meteor.settings.twilio.accountSid,
        Meteor.settings.twilio.authToken
      );
      
      const message = `Your verification code is ${otp}. This code expires in 5 minutes.`;
      
      const result = twilio.messages.create({
        body: message,
        from: Meteor.settings.twilio.phoneNumber,
        to: phoneNumber
      });
      
      return { success: true, messageId: result.sid };
    }
    
    // Example implementation with AWS SNS
    if (Meteor.settings.aws) {
      const AWS = require('aws-sdk');
      const sns = new AWS.SNS({
        accessKeyId: Meteor.settings.aws.accessKeyId,
        secretAccessKey: Meteor.settings.aws.secretAccessKey,
        region: Meteor.settings.aws.region
      });
      
      const message = `Your verification code is ${otp}. This code expires in 5 minutes.`;
      
      const params = {
        Message: message,
        PhoneNumber: phoneNumber
      };
      
      const result = sns.publish(params).promise();
      return { success: true, messageId: result.MessageId };
    }
    
    // If no SMS provider configured, log the OTP (development only)
    if (Meteor.isDevelopment) {
      console.log(`SMS to ${phoneNumber}: Your verification code is ${otp}`);
      return { success: true, messageId: 'dev-mode' };
    }
    
    throw new Error('No SMS provider configured');
    
  } catch (error) {
    console.error('SMS sending error:', error);
    return { success: false, error: error.message };
  }
}

// Collection definition
if (typeof OTPCollection === 'undefined') {
  OTPCollection = new Mongo.Collection('otp_verification');
  
  // Create indexes
  if (Meteor.isServer) {
    OTPCollection.createIndex({ phoneNumber: 1 });
    OTPCollection.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 });
  }
}

// Clean up expired OTPs periodically
if (Meteor.isServer) {
  Meteor.setInterval(function() {
    const expiredCount = OTPCollection.remove({
      expiresAt: { $lt: new Date() }
    });
    
    if (expiredCount > 0) {
      console.log(`Cleaned up ${expiredCount} expired OTP records`);
    }
  }, 60000); // Run every minute
}
