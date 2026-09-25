const mongoose = require("mongoose");

const KnowledgeSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
  localId: { type: Number, required: true },  // original Room DB id
  title: String,
  notes: String,
  youtubeLinks: String,
  confidence: String,
  tags: String,
  attachmentPaths: String,
  createdAt: Number,
  reviewCount: Number,
  lastTestScore: { type: Number, default: -1 },
  lastTestTotal: { type: Number, default: 0 },
  lastTestAt: { type: Number, default: 0 },
  isCompleted: { type: Boolean, default: false }
});

KnowledgeSchema.index({ userId: 1, localId: 1 }, { unique: true });

module.exports = mongoose.model("Knowledge", KnowledgeSchema);