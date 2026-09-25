const axios = require("axios");
async function sendEmail(to, subject, htmlContent) {
    try {
        await axios.post(
            "https://api.brevo.com/v3/smtp/email",
            {
                sender: {
                    name: "LearnKeep",
                    email: process.env.EMAIL_USER
                },
                to: [{ email: to }],
                subject: subject,
                htmlContent: htmlContent
            },
            {
                headers: {
                    "api-key": process.env.BREVO_API_KEY,
                    "Content-Type": "application/json"
                }
            }
        );
        console.log("✅ Email sent:", subject);
    } catch (error) {
        console.error("❌ Email error:", error.response?.data || error.message);
    }
}
module.exports = sendEmail;