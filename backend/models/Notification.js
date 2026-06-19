const mongoose = require('mongoose');

const NotificationSchema = new mongoose.Schema({
    coordinatorId: { type: String, required: true },
    senderName: { type: String },
    message: { type: String, required: true },
    attachmentUrl: { type: String },
    attachmentName: { type: String },
    attachmentMimeType: { type: String },
    createdAt: { type: Date, default: Date.now }
});

// TTL index to automatically delete notifications after 30 days (2592000 seconds)
NotificationSchema.index({ "createdAt": 1 }, { expireAfterSeconds: 2592000 });

module.exports = mongoose.model('Notification', NotificationSchema);
