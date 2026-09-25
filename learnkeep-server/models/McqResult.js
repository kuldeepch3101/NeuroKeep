const mongoose = require("mongoose");

const McqResultSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
  localId: { type: Number, required: true },
  topicId: Number,
  topicTitle: String,
  score: Number,
  total: Number,
  confidenceBefore: Number,
  confidenceAfter: Number,
  takenAt: Number,
  triggerType: String
});

McqResultSchema.index({ userId: 1, localId: 1 }, { unique: true });

module.exports = mongoose.model("McqResult", McqResultSchema);