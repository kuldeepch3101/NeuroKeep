const mongoose = require("mongoose");

const UserSchema = new mongoose.Schema({
name: String,
email: { type: String, unique: true},
password: String,
profilePic: { type: String, default: "" }
});

module.exports = mongoose.model("User", UserSchema);
