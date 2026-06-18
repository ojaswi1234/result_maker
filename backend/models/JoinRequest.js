const mongoose = require('mongoose');

const JoinRequestSchema = new mongoose.Schema({
    teacherName: { type: String, required: true },
    teacherGoogleId: { type: String, required: true },
    mobileNumber: { type: String, required: true },
    coordinatorId: { type: String, required: true },
    status: { type: String, enum: ["pending", "approved", "denied"], default: "pending" },
    createdAt: { type: Date, default: Date.now }
});

// TTL index to automatically delete requests after 24 hours (86400 seconds)
JoinRequestSchema.index({ "createdAt": 1 }, { expireAfterSeconds: 86400 });

module.exports = mongoose.model('JoinRequest', JoinRequestSchema);
