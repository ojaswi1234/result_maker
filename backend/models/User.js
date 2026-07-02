const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
    googleId: { type: String, required: true, unique: true },
    name: { type: String, required: true },
    mobileNumber: { type: String },
    role: { type: String, enum: ["Admin", "Teacher"], default: null },
    originalRole: { type: String, enum: ["Admin", "Teacher"], default: null },
    canToggleRole: { type: Boolean, default: false },
    coordinatorId: { type: String } // Links Teacher to Admin
});

module.exports = mongoose.model('User', UserSchema);
