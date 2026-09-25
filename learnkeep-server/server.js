const express    = require("express");
const mongoose   = require("mongoose");
const cors       = require("cors");
const jwt        = require("jsonwebtoken");
const axios      = require("axios");
const Knowledge = require("./models/Knowledge");
const McqResult = require("./models/McqResult");

require("dotenv").config();

const User           = require("./models/User");
const authMiddleware = require("./auth");
const sendEmail      = require("./emailService");
const otpGenerator   = require("otp-generator");
const otpStore       = {};
const app            = express();
const bcrypt         = require("bcrypt");
const JWT_SECRET     = process.env.JWT_SECRET;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

const {
    otpTemplate,
    resetOtpTemplate,
    welcomeTemplate,
    loginAlertTemplate,
    passwordChangedTemplate
} = require("./emailTemplates");

app.use(cors());
app.use(express.json({ limit: "5mb" }));

mongoose.connect(process.env.MONGO_URI)
    .then(() => console.log("MongoDB Connected"))
    .catch(err => console.log(err));

const GEMINI_MODELS =[
    "gemini-2.5-flash-lite",
    "gemini-1.5-flash-latest",
    "gemini-1.5-flash",
    "gemini-pro"
];

async function callGemini(prompt) {
    let lastError;
    for (const model of GEMINI_MODELS) {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`;
        try {
            const response = await axios.post(url, {
                contents: [{ parts: [{ text: prompt }] }]
            }, {
                headers: { "Content-Type": "application/json" },
                timeout: 30000
            });

            const text = response.data.candidates[0].content.parts[0].text;
            console.log(`✅ Gemini responded using model: ${model}`);
            return text;
        } catch (err) {
            const status = err.response?.status;
            const detail = err.response?.data?.error?.message || err.message;
            console.warn(`⚠️  Model ${model} failed (${status}): ${detail}`);
            lastError = `${model}: ${detail}`;

            if (status === 403 || status === 429) {
                throw new Error(`Gemini auth/quota error on ${model}: ${detail}`);
            }
        }
    }

    throw new Error(`All Gemini models failed. Last error: ${lastError}`);
}

app.post("/gemini/mcq", authMiddleware, async (req, res) => {
    try {
        const { title, notes, confidence } = req.body;
        if (!title) {
            return res.status(400).json({ success: false, message: "title is required" });
        }

        const prompt =
            `You are an expert educational assistant. Generate exactly 5 multiple-choice ` +
            `questions to test understanding of the topic: "${title}".\n` +
            (notes ? `Topic notes provided by the student:\n${notes}\n` : "") +
            `Current self-confidence level: ${confidence || 5}/10. ` +
            `Adjust difficulty — harder if confidence is high, gentler if low.\n\n` +
            `Return ONLY a valid JSON array — no markdown fences, no text outside the array:\n` +
            `[\n` +
            `  {\n` +
            `    "question": "...",\n` +
            `    "options": ["A", "B", "C", "D"],\n` +
            `    "correctIndex": 0,\n` +
            `    "explanation": "Short explanation of the correct answer"\n` +
            `  }\n` +
            `]\n` +
            `correctIndex is the 0-based index in the options array.`;
        const raw = await callGemini(prompt);

        let cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            const a = cleaned.indexOf("[");
            const b = cleaned.lastIndexOf("]");
            if (a >= 0 && b > a) cleaned = cleaned.substring(a, b + 1);
        }

        const questions = JSON.parse(cleaned);
        res.json({ success: true, questions });

    } catch (err) {
        console.error("MCQ error:", err.message);
        res.status(500).json({ success: false, message: err.message });
    }
});

app.post("/gemini/chat", authMiddleware, async (req, res) => {
    try {
        const { message, topics } = req.body;
        if (!message) {
            return res.status(400).json({ success: false, message: "message is required" });
        }

        let ctx = "You are LearnKeep AI — a smart, friendly study assistant.\n";
        ctx += "You have FULL access to the user's knowledge database:\n\n";

        if (!topics || topics.length === 0) {
            ctx += "(No topics saved yet)\n";
        } else {
            for (const t of topics) {
                ctx += `━ ${t.title}\n`;
                ctx += `  Confidence : ${t.confidence}/10\n`;
                ctx += `  Tag        : ${t.tags || "—"}\n`;
                if (t.notes) {
                    const preview = t.notes.length > 150
                        ? t.notes.substring(0, 150) + "…"
                        : t.notes;
                    ctx += `  Notes      : ${preview}\n`;
                }
                ctx += t.lastTestScore >= 0
                    ? `  Last score : ${t.lastTestScore}/${t.lastTestTotal}\n`
                    : `  Last score : No test yet\n`;
                ctx += `  Status     : ${t.isCompleted ? "✅ Completed" : "Active"}\n\n`;
            }
        }
        ctx += `User message: ${message}`;

        const reply = await callGemini(ctx);
        res.json({ success: true, reply });

    } catch (err) {
        console.error("Chat error:", err.message);
        res.status(500).json({ success: false, message: err.message });
    }
});

app.get("/gemini/ping", async (req, res) => {
    try {
        const reply = await callGemini("Say exactly: OK");
        res.json({ success: true, reply });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

app.post("/signup", async (req, res) => {
    const { name, email, password, otp } = req.body;
    if (!otpStore[email] ||
        otpStore[email].otp !== otp ||
        otpStore[email].expires < Date.now()) {
        return res.json({ success: false, message: "Invalid or expired OTP" });
    }
    const hashedPassword = await bcrypt.hash(password, 10);
    const user = new User({ name, email, password: hashedPassword });
    await user.save();
    delete otpStore[email];
    await sendEmail(email, "Welcome to LearnKeep 🎉", welcomeTemplate(name));
    res.json({ success: true });
});

app.post("/send-signup-otp", async (req, res) => {
    const { email } = req.body;
    const otp = otpGenerator.generate(6, {
        digits: true, lowerCaseAlphabets: false,
        upperCaseAlphabets: false, specialChars: false
    });
    otpStore[email] = { otp, expires: Date.now() + 5 * 60 * 1000 };
    await sendEmail(email, "Verify Your Account", otpTemplate(otp));
    res.json({ success: true });
});

app.post("/login", async (req, res) => {
    const { email, password } = req.body;
    const user = await User.findOne({ email });
    if (!user) return res.json({ success: false, message: "User not found" });
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.json({ success: false });
    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, { expiresIn: "7d" });
    await sendEmail(email, "⚠️ Login Alert", loginAlertTemplate());
    res.json({ success: true, token, name: user.name, email: user.email });
});

app.post("/send-login-otp", async (req, res) => {
    const { email } = req.body;
    const otp = otpGenerator.generate(6, {
        digits: true, lowerCaseAlphabets: false,
        upperCaseAlphabets: false, specialChars: false
    });
    otpStore[email] = { otp, expires: Date.now() + 5 * 60 * 1000 };
    await sendEmail(email, "Login Verification Code", otpTemplate(otp));
    res.json({ success: true });
});

app.get("/profile", authMiddleware, async (req, res) => {
    try {
        const user = await User.findById(req.user.id);
        res.json({ name: user.name, email: user.email, profilePic: user.profilePic || "" });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

app.post("/forgot-password-otp", async (req, res) => {
    const { email } = req.body;
    const otp = otpGenerator.generate(6, {
        digits: true, lowerCaseAlphabets: false,
        upperCaseAlphabets: false, specialChars: false
    });
    otpStore[email] = { otp, expires: Date.now() + 5 * 60 * 1000 };
    await sendEmail(email, "Reset Password Code", resetOtpTemplate(otp));
    res.json({ success: true });
});

app.post("/reset-password", async (req, res) => {
    const { email, otp, newPassword } = req.body;
    if (!otpStore[email] ||
        otpStore[email].otp !== otp ||
        otpStore[email].expires < Date.now()) {
        return res.json({ success: false, message: "Invalid or expired OTP" });
    }
    const hashedPassword = await bcrypt.hash(newPassword, 10);
    await User.updateOne({ email }, { password: hashedPassword });
    delete otpStore[email];
    await sendEmail(email, "Password Changed", passwordChangedTemplate());
    res.json({ success: true });
});

app.put("/update-profile-pic", authMiddleware, async (req, res) => {
    const { profilePic } = req.body;
    if (!profilePic) return res.json({ success: false, message: "No image data provided" });
    try {
        await User.updateOne({ _id: req.user.id }, { profilePic });
        res.json({ success: true, message: "Profile picture updated" });
    } catch (err) {
        console.error("Profile pic update error:", err);
        res.status(500).json({ success: false });
    }
});
// ── SYNC: Upload all topics from device → MongoDB ──────────────────
app.post("/sync/knowledge", authMiddleware, async (req, res) => {
  try {
    const { topics } = req.body; // array of KnowledgeEntity objects
    if (!topics || !Array.isArray(topics)) {
      return res.status(400).json({ success: false, message: "topics array required" });
    }

    const userId = req.user.id;
    const ops = topics.map(t => ({
      updateOne: {
        filter: { userId, localId: t.id },
        update: { $set: { ...t, userId, localId: t.id } },
        upsert: true  // insert if not exists, update if exists
      }
    }));

    await Knowledge.bulkWrite(ops);
    res.json({ success: true, synced: topics.length });
  } catch (err) {
    console.error("Knowledge sync error:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// ── SYNC: Upload all MCQ results from device → MongoDB ─────────────
app.post("/sync/mcq-results", authMiddleware, async (req, res) => {
  try {
    const { results } = req.body;
    if (!results || !Array.isArray(results)) {
      return res.status(400).json({ success: false, message: "results array required" });
    }

    const userId = req.user.id;
    const ops = results.map(r => ({
      updateOne: {
        filter: { userId, localId: r.id },
        update: { $set: { ...r, userId, localId: r.id } },
        upsert: true
      }
    }));

    await McqResult.bulkWrite(ops);
    res.json({ success: true, synced: results.length });
  } catch (err) {
    console.error("MCQ sync error:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// ── RESTORE: Fetch all topics for this user from MongoDB ───────────
app.get("/sync/knowledge", authMiddleware, async (req, res) => {
  try {
    const topics = await Knowledge.find({ userId: req.user.id }).lean();
    res.json({ success: true, topics });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// ── RESTORE: Fetch all MCQ results for this user from MongoDB ──────
app.get("/sync/mcq-results", authMiddleware, async (req, res) => {
  try {
    const results = await McqResult.find({ userId: req.user.id }).lean();
    res.json({ success: true, results });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
