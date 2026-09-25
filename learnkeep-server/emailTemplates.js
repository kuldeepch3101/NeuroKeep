function otpTemplate(otp) {
    return `
    <div style="background-color: #f0f0f3; padding: 50px 20px; font-family: Arial, sans-serif;">
        <div style="max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 20px; box-shadow: 10px 10px 30px rgba(174, 174, 192, 0.4), -10px -10px 30px rgba(255, 255, 255, 1); padding: 50px 40px; text-align: center;">

            <div style="margin-bottom: 25px;">
                <div style="font-size: 28px; font-weight: bold; color: #6a46b3;">LearnKeep</div>
            </div>

            <h3 style="color: #1a1a1a; margin-top: 0; font-weight: bold; font-size: 18px;">Verification Required</h3>
            <p style="color: #555; font-size: 14px; line-height: 1.5; margin-bottom: 30px;">
                Your code is ready. Enter it to confirm your identity.
            </p>

            <div style="font-size: 38px; font-weight: bold; color: #6a46b3; letter-spacing: 8px; margin: 0;">
                ${otp}
            </div>

            <p style="color: #888; font-size: 12px; margin-top: 40px; margin-bottom: 0;">
                Code is valid for 5 minutes.
            </p>
        </div>
    </div>`;
}

function resetOtpTemplate(otp) {
    return `
    <div style="background-color: #f0f0f3; padding: 50px 20px; font-family: Arial, sans-serif;">
        <div style="max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 20px; box-shadow: 10px 10px 30px rgba(174, 174, 192, 0.4), -10px -10px 30px rgba(255, 255, 255, 1); padding: 50px 40px; text-align: center;">

            <div style="margin-bottom: 25px;">
                <div style="font-size: 28px; font-weight: bold; color: #6a46b3;">LearnKeep</div>
            </div>

            <h3 style="color: #EF4444; margin-top: 0; font-weight: bold; font-size: 20px;">🔐 Reset Password</h3>
            <p style="color: #555; font-size: 14px; line-height: 1.5; margin-bottom: 30px;">
                We received a request to reset the password for your account. Enter the code below to proceed:
            </p>

            <div style="font-size: 38px; font-weight: bold; color: #EF4444; letter-spacing: 8px; margin: 0;">
                ${otp}
            </div>

            <div style="margin-top: 40px; border-top: 1px solid #f0f0f0; padding-top: 20px;">
                <p style="color: #888; font-size: 12px; margin-bottom: 8px;">
                    <strong>This code expires in 5 minutes.</strong>
                </p>
                <p style="color: #aaa; font-size: 11px; line-height: 1.4; margin-top: 0;">
                    If you did not request a password reset, someone else may be typing your email by mistake. You can safely ignore this email.
                </p>
            </div>
        </div>
    </div>`;
}

function welcomeTemplate(name) {
    return `
    <div style="background-color: #f0f0f3; padding: 50px 20px; font-family: Arial, sans-serif;">
        <div style="max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 20px; box-shadow: 10px 10px 30px rgba(174, 174, 192, 0.4), -10px -10px 30px rgba(255, 255, 255, 1); padding: 50px 40px; text-align: center;">
            <div style="margin-bottom: 25px;">
                <div style="font-size: 28px; font-weight: bold; color: #6a46b3;">LearnKeep</div>
            </div>
            <h3 style="color: #1a1a1a; margin-top: 0; font-weight: bold; font-size: 22px;">🎉 Welcome to LearnKeep!</h3>
            <p style="color: #555; font-size: 15px; line-height: 1.6; margin-bottom: 30px;">
                Hi ${name},<br><br>
                Your email has been verified and your account is successfully created. We are thrilled to have you on board!
            </p>
            <div style="background-color: #f9f7ff; border: 1px solid #e8e0ff; border-radius: 12px; padding: 20px; margin-bottom: 30px;">
                <p style="color: #6a46b3; font-size: 16px; font-weight: bold; line-height: 1.5; margin: 0;">
                    Start tracking your knowledge and grow smarter every day 🚀
                </p>
            </div>
            <a href="#" style="display: inline-block; background-color: #6a46b3; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 10px; font-weight: bold; font-size: 15px;">
                Go to Dashboard
            </a>
            <div style="margin-top: 40px; border-top: 1px solid #f0f0f0; padding-top: 20px;">
                <p style="color: #888; font-size: 12px; margin-bottom: 8px;">
                    <strong>Need help getting started?</strong>
                </p>
                <p style="color: #aaa; font-size: 11px; line-height: 1.4; margin-top: 0;">
                    Just reply to this email to get in touch with our support team. We're here to help!
                </p>
            </div>
        </div>
    </div>`;
}

function loginAlertTemplate() {
    return `
    <div style="background-color: #f0f0f3; padding: 50px 20px; font-family: Arial, sans-serif;">
        <div style="max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 20px; box-shadow: 10px 10px 30px rgba(174, 174, 192, 0.4), -10px -10px 30px rgba(255, 255, 255, 1); padding: 50px 40px; text-align: center;">

            <!-- Logo -->
            <div style="margin-bottom: 25px;">
                <div style="font-size: 28px; font-weight: bold; color: #6a46b3;">LearnKeep</div>
            </div>

            <!-- Alert Heading -->
            <h3 style="color: #DC2626; margin-top: 0; font-weight: bold; font-size: 22px;">⚠️ Security Alert</h3>

            <!-- Alert Text -->
            <p style="color: #555; font-size: 15px; line-height: 1.6; margin-bottom: 30px;">
                We noticed a new login to your LearnKeep account.

            </p>

            <!-- Highlighted Warning Statement -->
            <div style="background-color: #FEF2F2; border: 1px solid #FCA5A5; border-radius: 12px; padding: 20px; margin-bottom: 30px;">
                <p style="color: #DC2626; font-size: 16px; font-weight: bold; line-height: 1.5; margin: 0;">
                    If this wasn't you, please secure your account immediately.
                </p>
            </div>

            <!-- CTA Button -->
            <a href="#" style="display: inline-block; background-color: #DC2626; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 10px; font-weight: bold; font-size: 15px;">
                Reset Password
            </a>

            <!-- Footer / Support -->
            <div style="margin-top: 40px; border-top: 1px solid #f0f0f0; padding-top: 20px;">
                <p style="color: #888; font-size: 12px; margin-bottom: 8px;">
                    <strong>Why did I get this?</strong>
                </p>
                <p style="color: #aaa; font-size: 11px; line-height: 1.4; margin-top: 0;">
                    We send these alerts to help protect your account. If you need assistance, reply to this email to reach our support team.
                </p>
            </div>
        </div>
    </div>`;
}

function passwordChangedTemplate() {
    return `
    <div style="background-color: #f0f0f3; padding: 50px 20px; font-family: Arial, sans-serif;">
        <div style="max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 20px; box-shadow: 10px 10px 30px rgba(174, 174, 192, 0.4), -10px -10px 30px rgba(255, 255, 255, 1); padding: 50px 40px; text-align: center;">

            <!-- Logo -->
            <div style="margin-bottom: 25px;">
                <div style="font-size: 28px; font-weight: bold; color: #6a46b3;">LearnKeep</div>
            </div>

            <!-- Success Heading -->
            <h3 style="color: #16A34A; margin-top: 0; font-weight: bold; font-size: 22px;">✅ Password Updated</h3>

            <!-- Confirmation Text -->
            <p style="color: #555; font-size: 15px; line-height: 1.6; margin-bottom: 30px;">
                The password for your LearnKeep account has been successfully changed.<br><br>
                You can now use your new password to log in across all your devices.
            </p>


            <!-- CTA Button -->
            <a href="#" style="display: inline-block; background-color: #16A34A; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 10px; font-weight: bold; font-size: 15px;">
                Log In Now
            </a>

            <!-- Footer / Support -->
            <div style="margin-top: 40px; border-top: 1px solid #f0f0f0; padding-top: 20px;">
                <p style="color: #888; font-size: 12px; margin-bottom: 8px;">
                    <strong>Need to reach us?</strong>
                </p>
                <p style="color: #aaa; font-size: 11px; line-height: 1.4; margin-top: 0;">
                    Reply directly to this email to get in touch with our support team to secure your account.
                </p>
            </div>
        </div>
    </div>`;
}

module.exports = {
    otpTemplate,
    resetOtpTemplate,
    welcomeTemplate,
    loginAlertTemplate,
    passwordChangedTemplate
};