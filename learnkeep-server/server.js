
const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const jwt = require("jsonwebtoken");
const axios = require("axios");
const bcrypt = require("bcrypt");
const otpGenerator = require("otp-generator");

require("dotenv").config();

const Knowledge = require("./models/Knowledge");
const McqResult = require("./models/McqResult");
const User = require("./models/User");
const authMiddleware = require("./auth");
const sendEmail = require("./emailService");

const {
  otpTemplate,
  resetOtpTemplate,
  welcomeTemplate,
  loginAlertTemplate,
  passwordChangedTemplate,
} = require("./emailTemplates");

const app = express();
const otpStore = {};

const JWT_SECRET = process.env.JWT_SECRET;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

app.use(cors());
app.use(express.json({ limit: "5mb" }));

// --------------------------------------------------
// MONGODB CONNECTION
// --------------------------------------------------

mongoose
  .connect(process.env.MONGO_URI)
  .then(() => console.log("MongoDB Connected"))
  .catch((err) => console.error("MongoDB connection error:", err.message));

// --------------------------------------------------
// GEMINI CONFIGURATION
// --------------------------------------------------

// Keep models that are available to your API key.
// If a model is unavailable, the server tries the next one.
const GEMINI_MODELS = [
    "gemini-3.5-flash",
    "gemini-3.5-flash-latest",
    "gemini-3.5-flash-lite",
    "gemini-2.5-flash-lite",
    "gemini-1.5-flash-latest",
    "gemini-1.5-flash",
    "gemini-pro"
];

async function callGemini(prompt, jsonMode = false) {
  if (!GEMINI_API_KEY) {
    throw new Error("GEMINI_API_KEY is missing from .env");
  }

  let lastError;

  for (const model of GEMINI_MODELS) {
    const url =
      `https://generativelanguage.googleapis.com/v1beta/models/` +
      `${model}:generateContent?key=${GEMINI_API_KEY}`;

    try {
      const generationConfig = {
        temperature: 0.2,
        maxOutputTokens: 8192,
      };

      // Ask Gemini to enforce JSON output for MCQs.
      if (jsonMode) {
        generationConfig.responseMimeType = "application/json";
      }

      const response = await axios.post(
        url,
        {
          contents: [
            {
              role: "user",
              parts: [{ text: prompt }],
            },
          ],
          generationConfig,
        },
        {
          headers: { "Content-Type": "application/json" },
          timeout: 60000,
        }
      );

      const candidate = response.data?.candidates?.[0];
      const text = candidate?.content?.parts
        ?.map((part) => part.text || "")
        .join("");

      if (!text) {
        const reason = candidate?.finishReason || "No response text";
        throw new Error(`Gemini returned no text: ${reason}`);
      }

      console.log(`Gemini responded using model: ${model}`);
      return text;
    } catch (err) {
      const status = err.response?.status;
      const detail =
        err.response?.data?.error?.message || err.message;

      console.error(
        `Gemini model ${model} failed (${status || "unknown"}): ${detail}`
      );

      lastError = `${model}: ${detail}`;

      // These errors generally will not be fixed by changing models.
      if (
        status === 400 ||
        status === 401 ||
        status === 403 ||
        status === 429
      ) {
        throw new Error(`Gemini API error: ${detail}`);
      }
    }
  }

  throw new Error(`All Gemini models failed. ${lastError || ""}`);
}

// --------------------------------------------------
// MCQ JSON PARSER AND VALIDATOR
// --------------------------------------------------

function parseMcqResponse(raw) {
  let cleaned = raw.trim();

  // Remove optional Markdown code fences.
  cleaned = cleaned
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```\s*$/i, "")
    .trim();

  // Extract the array if Gemini includes surrounding text.
  const start = cleaned.indexOf("[");
  const end = cleaned.lastIndexOf("]");

  if (start === -1 || end === -1 || end < start) {
    throw new Error("Gemini did not return a JSON array.");
  }

  cleaned = cleaned.slice(start, end + 1);

  let questions;

  try {
    questions = JSON.parse(cleaned);
  } catch (err) {
    // Do not attempt unsafe regex-based JSON repair.
    console.error("MCQ JSON parsing failed:", err.message);
    console.error("Malformed response:", raw);

    throw new Error(
      "Gemini generated invalid MCQ JSON. Please try again."
    );
  }

  if (!Array.isArray(questions) || questions.length !== 5) {
    throw new Error("Gemini must return exactly 5 questions.");
  }

  const valid = questions.every(
    (q) =>
      q &&
      typeof q.question === "string" &&
      q.question.trim().length > 0 &&
      Array.isArray(q.options) &&
      q.options.length === 4 &&
      q.options.every(
        (option) =>
          typeof option === "string" && option.trim().length > 0
      ) &&
      Number.isInteger(q.correctIndex) &&
      q.correctIndex >= 0 &&
      q.correctIndex <= 3 &&
      typeof q.explanation === "string"
  );

  if (!valid) {
    throw new Error("Gemini returned an invalid MCQ structure.");
  }

  return questions;
}

// --------------------------------------------------
// GEMINI MCQ GENERATION
// --------------------------------------------------

app.post("/gemini/mcq", authMiddleware, async (req, res) => {
  try {
    const { title, notes, confidence } = req.body;

    if (typeof title !== "string" || !title.trim()) {
      return res.status(400).json({
        success: false,
        message: "A topic title is required.",
      });
    }

    const confidenceValue = Number(confidence ?? 5);
    const safeConfidence = Number.isFinite(confidenceValue)
      ? Math.min(10, Math.max(1, confidenceValue))
      : 5;

    const prompt = `
You are LearnKeep AI, an educational assistant.

Generate exactly 5 multiple-choice questions about this topic:
${title}

Student notes:
${typeof notes === "string" ? notes : "No notes provided"}

Student confidence: ${safeConfidence}/10.
Adjust difficulty according to confidence.

Return a JSON array containing exactly 5 objects.
Each object must have exactly these fields:
- "question": a string
- "options": an array of exactly 4 strings
- "correctIndex": an integer from 0 to 3
- "explanation": a string

Important:
- Output valid JSON only.
- Escape quotation marks inside strings correctly.
- Never put unescaped line breaks inside a JSON string.
- Do not include Markdown fences or commentary.
- Ensure every question has one correct answer.
- correctIndex is zero-based.

Example:
[
  {
    "question": "What is cloud computing?",
    "options": [
      "Using remote computing resources",
      "Using only local storage",
      "Turning off a computer",
      "Writing documents offline"
    ],
    "correctIndex": 0,
    "explanation": "Cloud computing provides computing resources over a network."
  }
]
`;

    // Retry once if Gemini produces malformed JSON.
    let questions;
    let lastError;

    for (let attempt = 1; attempt <= 2; attempt++) {
      try {
        const raw = await callGemini(prompt, true);
        questions = parseMcqResponse(raw);
        break;
      } catch (err) {
        lastError = err;
        console.error(`MCQ attempt ${attempt} failed:`, err.message);

        // Do not retry API authentication or quota errors.
        if (
          /Gemini API error:/.test(err.message)
        ) {
          throw err;
        }

        if (attempt === 2) {
          throw err;
        }
      }
    }

    if (!questions) {
      throw lastError || new Error("MCQ generation failed.");
    }

    return res.json({
      success: true,
      questions,
    });
  } catch (err) {
    console.error("MCQ error:", err.message);

    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
});

// --------------------------------------------------
// GEMINI STUDY CHAT
// --------------------------------------------------

app.post("/gemini/chat", authMiddleware, async (req, res) => {
  try {
    const { message, topics } = req.body;

    if (typeof message !== "string" || !message.trim()) {
      return res.status(400).json({
        success: false,
        message: "A message is required.",
      });
    }

    let ctx =
      "You are LearnKeep AI, a smart and friendly study assistant.\n";
    ctx += "Use the following saved knowledge to personalize answers:\n\n";

    if (!Array.isArray(topics) || topics.length === 0) {
      ctx += "(No topics saved yet)\n";
    } else {
      for (const t of topics) {
        ctx += `Topic: ${t.title || "Untitled"}\n`;
        ctx += `Confidence: ${t.confidence ?? "Unknown"}/10\n`;
        ctx += `Tag: ${t.tags || "—"}\n`;

        if (t.notes) {
          const preview =
            t.notes.length > 150
              ? t.notes.substring(0, 150) + "..."
              : t.notes;

          ctx += `Notes: ${preview}\n`;
        }

        ctx +=
          typeof t.lastTestScore === "number" && t.lastTestScore >= 0
            ? `Last score: ${t.lastTestScore}/${t.lastTestTotal}\n`
            : "Last score: No test yet\n";

        ctx += `Status: ${
          t.isCompleted ? "Completed" : "Active"
        }\n\n`;
      }
    }

    ctx += `User message: ${message}`;

    const reply = await callGemini(ctx);

    return res.json({ success: true, reply });
  } catch (err) {
    console.error("Chat error:", err.message);

    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
});

// --------------------------------------------------
// GEMINI CONNECTION TEST
// --------------------------------------------------

app.get("/gemini/ping", async (req, res) => {
  try {
    const reply = await callGemini("Say exactly: OK");

    return res.json({ success: true, reply });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
});

// --------------------------------------------------
// SIGNUP
// --------------------------------------------------

app.post("/signup", async (req, res) => {
  try {
    const { name, email, password, otp } = req.body;

    if (!name || !email || !password || !otp) {
      return res.status(400).json({
        success: false,
        message: "Name, email, password and OTP are required.",
      });
    }

    const storedOtp = otpStore[email];

    if (
      !storedOtp ||
      storedOtp.otp !== String(otp) ||
      storedOtp.expires < Date.now()
    ) {
      return res.status(400).json({
        success: false,
        message: "Invalid or expired OTP.",
      });
    }

    const existingUser = await User.findOne({ email });

    if (existingUser) {
      return res.status(409).json({
        success: false,
        message: "An account with this email already exists.",
      });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const user = new User({
      name,
      email,
      password: hashedPassword,
    });

    await user.save();
    delete otpStore[email];

    await sendEmail(
      email,
      "Welcome to LearnKeep",
      welcomeTemplate(name)
    );

    return res.json({ success: true });
  } catch (err) {
    console.error("Signup error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Signup failed.",
    });
  }
});

// --------------------------------------------------
// SEND SIGNUP OTP
// --------------------------------------------------

app.post("/send-signup-otp", async (req, res) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: "Email is required.",
      });
    }

    const otp = otpGenerator.generate(6, {
      digits: true,
      lowerCaseAlphabets: false,
      upperCaseAlphabets: false,
      specialChars: false,
    });

    await sendEmail(
      email,
      "Verify Your Account",
      otpTemplate(otp)
    );

    otpStore[email] = {
      otp,
      expires: Date.now() + 5 * 60 * 1000,
    };

    return res.json({ success: true });
  } catch (err) {
    console.error("Signup OTP error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Unable to send signup OTP.",
    });
  }
});

// --------------------------------------------------
// LOGIN
// --------------------------------------------------

app.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: "Email and password are required.",
      });
    }

    const user = await User.findOne({ email });

    if (!user) {
      return res.status(401).json({
        success: false,
        message: "Invalid email or password.",
      });
    }

    const isMatch = await bcrypt.compare(password, user.password);

    if (!isMatch) {
      return res.status(401).json({
        success: false,
        message: "Invalid email or password.",
      });
    }

    if (!JWT_SECRET) {
      throw new Error("JWT_SECRET is missing from .env");
    }

    const token = jwt.sign(
      { id: user._id },
      JWT_SECRET,
      { expiresIn: "7d" }
    );

    await sendEmail(
      email,
      "Login Alert",
      loginAlertTemplate()
    );

    return res.json({
      success: true,
      token,
      name: user.name,
      email: user.email,
    });
  } catch (err) {
    console.error("Login error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Login failed.",
    });
  }
});

// --------------------------------------------------
// SEND LOGIN OTP
// --------------------------------------------------

app.post("/send-login-otp", async (req, res) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: "Email is required.",
      });
    }

    const otp = otpGenerator.generate(6, {
      digits: true,
      lowerCaseAlphabets: false,
      upperCaseAlphabets: false,
      specialChars: false,
    });

    await sendEmail(
      email,
      "Login Verification Code",
      otpTemplate(otp)
    );

    otpStore[email] = {
      otp,
      expires: Date.now() + 5 * 60 * 1000,
    };

    return res.json({ success: true });
  } catch (err) {
    console.error("Login OTP error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Unable to send login OTP.",
    });
  }
});

// --------------------------------------------------
// PROFILE
// --------------------------------------------------

app.get("/profile", authMiddleware, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    return res.json({
      name: user.name,
      email: user.email,
      profilePic: user.profilePic || "",
    });
  } catch (err) {
    console.error("Profile error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Unable to load profile.",
    });
  }
});

// --------------------------------------------------
// FORGOT PASSWORD OTP
// --------------------------------------------------

app.post("/forgot-password-otp", async (req, res) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: "Email is required.",
      });
    }

    const user = await User.findOne({ email });

    if (!user) {
      return res.json({
        success: true,
        message: "If the account exists, a reset code will be sent.",
      });
    }

    const otp = otpGenerator.generate(6, {
      digits: true,
      lowerCaseAlphabets: false,
      upperCaseAlphabets: false,
      specialChars: false,
    });

    await sendEmail(
      email,
      "Reset Password Code",
      resetOtpTemplate(otp)
    );

    otpStore[email] = {
      otp,
      expires: Date.now() + 5 * 60 * 1000,
    };

    return res.json({ success: true });
  } catch (err) {
    console.error("Forgot password error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Unable to process password reset.",
    });
  }
});

// --------------------------------------------------
// RESET PASSWORD
// --------------------------------------------------

app.post("/reset-password", async (req, res) => {
  try {
    const { email, otp, newPassword } = req.body;
    const storedOtp = otpStore[email];

    if (!email || !otp || !newPassword) {
      return res.status(400).json({
        success: false,
        message: "Email, OTP and new password are required.",
      });
    }

    if (
      !storedOtp ||
      storedOtp.otp !== String(otp) ||
      storedOtp.expires < Date.now()
    ) {
      return res.status(400).json({
        success: false,
        message: "Invalid or expired OTP.",
      });
    }

    const hashedPassword = await bcrypt.hash(newPassword, 10);

    const result = await User.updateOne(
      { email },
      { $set: { password: hashedPassword } }
    );

    if (!result.matchedCount) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    delete otpStore[email];

    await sendEmail(
      email,
      "Password Changed",
      passwordChangedTemplate()
    );

    return res.json({ success: true });
  } catch (err) {
    console.error("Reset password error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Unable to reset password.",
    });
  }
});

// --------------------------------------------------
// UPDATE PROFILE PICTURE
// --------------------------------------------------

app.put("/update-profile-pic", authMiddleware, async (req, res) => {
  try {
    const { profilePic } = req.body;

    if (!profilePic) {
      return res.status(400).json({
        success: false,
        message: "No image data provided.",
      });
    }

    await User.updateOne(
      { _id: req.user.id },
      { $set: { profilePic } }
    );

    return res.json({
      success: true,
      message: "Profile picture updated.",
    });
  } catch (err) {
    console.error("Profile picture error:", err.message);

    return res.status(500).json({
      success: false,
      message: "Unable to update profile picture.",
    });
  }
});

// --------------------------------------------------
// SYNC KNOWLEDGE: DEVICE -> MONGODB
// --------------------------------------------------

app.post("/sync/knowledge", authMiddleware, async (req, res) => {
  try {
    const { topics } = req.body;

    if (!Array.isArray(topics)) {
      return res.status(400).json({
        success: false,
        message: "topics array required.",
      });
    }

    if (topics.length === 0) {
      return res.json({ success: true, synced: 0 });
    }

    const userId = req.user.id;

    const ops = topics.map((t) => ({
      updateOne: {
        filter: { userId, localId: t.id },
        update: {
          $set: {
            ...t,
            userId,
            localId: t.id,
          },
        },
        upsert: true,
      },
    }));

    await Knowledge.bulkWrite(ops);

    return res.json({
      success: true,
      synced: topics.length,
    });
  } catch (err) {
    console.error("Knowledge sync error:", err.message);

    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
});

// --------------------------------------------------
// SYNC MCQ RESULTS: DEVICE -> MONGODB
// --------------------------------------------------

app.post("/sync/mcq-results", authMiddleware, async (req, res) => {
  try {
    const { results } = req.body;

    if (!Array.isArray(results)) {
      return res.status(400).json({
        success: false,
        message: "results array required.",
      });
    }

    if (results.length === 0) {
      return res.json({ success: true, synced: 0 });
    }

    const userId = req.user.id;

    const ops = results.map((r) => ({
      updateOne: {
        filter: { userId, localId: r.id },
        update: {
          $set: {
            ...r,
            userId,
            localId: r.id,
          },
        },
        upsert: true,
      },
    }));

    await McqResult.bulkWrite(ops);

    return res.json({
      success: true,
      synced: results.length,
    });
  } catch (err) {
    console.error("MCQ results sync error:", err.message);

    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
});

// --------------------------------------------------
// RESTORE KNOWLEDGE: MONGODB -> DEVICE
// --------------------------------------------------

app.get("/sync/knowledge", authMiddleware, async (req, res) => {
  try {
    const topics = await Knowledge.find({
      userId: req.user.id,
    }).lean();

    return res.json({
      success: true,
      topics,
    });
  } catch (err) {
    console.error("Knowledge restore error:", err.message);

    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
});

// --------------------------------------------------
// RESTORE MCQ RESULTS: MONGODB -> DEVICE
// --------------------------------------------------

app.get("/sync/mcq-results", authMiddleware, async (req, res) => {
  try {
    const results = await McqResult.find({
      userId: req.user.id,
    }).lean();

    return res.json({
      success: true,
      results,
    });
  } catch (err) {
    console.error("MCQ results restore error:", err.message);

    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
});

// --------------------------------------------------
// START SERVER
// --------------------------------------------------

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
