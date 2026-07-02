require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const mongoose = require('mongoose');
const socketManager = require('./sockets/socketManager');
const JoinRequest = require('./models/JoinRequest');
const User = require('./models/User');
const Notification = require('./models/Notification');
const path = require('path');
const fs = require('fs');
const multer = require('multer');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

const MONGO_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/resultmaker';

mongoose.connect(MONGO_URI)
    .then(() => console.log('Connected to MongoDB successfully'))
    .catch(err => console.error('MongoDB connection error:', err));

app.use(express.json());

/**
 * REST Endpoint: Sync/Create a user in MongoDB.
 * Make it idempotent and upsert the record.
 */
app.post('/users/sync', async (req, res) => {
    try {
        const { googleId, name, mobileNumber, role, coordinatorId } = req.body;
        if (!googleId) {
            return res.status(400).json({ error: 'googleId is required' });
        }

        let user = await User.findOne({ googleId });
        
        if (!user) {
            const canToggleRole = (role === 'Admin');
            user = await User.create({
                googleId,
                name: name || '',
                mobileNumber,
                role,
                originalRole: role,
                canToggleRole,
                coordinatorId
            });
            return res.json(user);
        } else {
            const updateData = {};
            if (name !== undefined) updateData.name = name;
            if (mobileNumber !== undefined) updateData.mobileNumber = mobileNumber;
            if (coordinatorId !== undefined) updateData.coordinatorId = coordinatorId;
            
            if (role !== undefined) {
                if (user.originalRole === 'Teacher' && role === 'Admin') {
                    // Do nothing, a registered Teacher cannot switch to Admin
                } else if (user.canToggleRole || user.role === role) {
                    // Update if they have privilege, or if they are just sending their current role
                    updateData.role = role;
                }
            }
            
            const updatedUser = await User.findOneAndUpdate(
                { googleId },
                updateData,
                { new: true }
            );
            return res.json(updatedUser);
        }
    } catch (err) {
        console.error('Error syncing user:', err);
        res.status(500).json({ error: 'Internal Server Error' });
    }
});

/**
 * REST Endpoint: Fetch approved teachers for a coordinator.
 */
app.get('/teachers/:coordinatorId', async (req, res) => {
    try {
        const { coordinatorId } = req.params;
        const teachers = await User.find({ coordinatorId, role: "Teacher" }).lean();
        
        // Serialize _id as string for Android compatibility
        const serializedTeachers = teachers.map(teacher => ({
            ...teacher,
            _id: teacher._id.toString()
        }));
        
        res.json(serializedTeachers);
    } catch (err) {
        console.error('Error fetching approved teachers:', err);
        res.status(500).json({ error: 'Internal Server Error' });
    }
});

/**
 * REST Endpoint: Fetch pending join requests for a coordinator.
 * Fix for Bug 1: Recovery when backend restarts and socket rooms are lost.
 */
app.get('/pending-requests/:coordinatorId', async (req, res) => {
    try {
        const { coordinatorId } = req.params;
        // Fetch only pending requests for this coordinator
        const requests = await JoinRequest.find({ coordinatorId, status: 'pending' }).lean();
        
        // Serialize _id as string for Android compatibility
        const serializedRequests = requests.map(request => ({
            ...request,
            _id: request._id.toString()
        }));
        
        res.json(serializedRequests);
    } catch (err) {
        console.error('Error fetching pending requests:', err);
        res.status(500).json({ error: 'Internal Server Error' });
    }
});


/**
 * REST Endpoint: Check status of a teacher's join request / approval status.
 * Used for recovery when the app was closed during approval.
 */
app.get('/request-status/:teacherGoogleId', async (req, res) => {
    try {
        const { teacherGoogleId } = req.params;
        
        // 1. Check if there's a user record (already approved)
        const user = await User.findOne({ googleId: teacherGoogleId, role: "Teacher" });
        if (user) {
            return res.json({ 
                status: 'approved', 
                coordinatorId: user.coordinatorId 
            });
        }
        
        // 2. Check if the request is still pending
        const pendingRequest = await JoinRequest.findOne({ teacherGoogleId, status: 'pending' });
        if (pendingRequest) {
            return res.json({ 
                status: 'pending' 
            });
        }
        
        // 3. Neither exists (denied or deleted)
        res.json({ 
            status: 'none' 
        });
    } catch (err) {
        console.error('Error checking request status:', err);
        res.status(500).json({ error: 'Internal Server Error' });
    }
});

/**
 * REST Endpoint: Fetch coordinator's name/info.
 */
app.get('/coordinator-info/:coordinatorId', async (req, res) => {
    try {
        const { coordinatorId } = req.params;
        const coordinator = await User.findOne({ coordinatorId, role: "Admin" });
        if (coordinator) {
            res.json({ name: coordinator.name });
        } else {
            res.status(404).json({ error: 'Coordinator not found' });
        }
    } catch (err) {
        console.error('Error fetching coordinator info:', err);
        res.status(500).json({ error: 'Internal Server Error' });
    }
});

// Create uploads directory if it doesn't exist
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Multer storage configuration
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        const ext = path.extname(file.originalname);
        cb(null, file.fieldname + '-' + uniqueSuffix + ext);
    }
});

// File filter to reject dangerous executables/scripts
const fileFilter = (req, file, cb) => {
    const forbiddenExtensions = ['.exe', '.bat', '.sh', '.js', '.vbs', '.scr', '.com', '.msi'];
    const ext = path.extname(file.originalname).toLowerCase();
    const mime = file.mimetype ? file.mimetype.toLowerCase() : '';
    
    if (forbiddenExtensions.includes(ext) || mime.includes('javascript') || mime.includes('x-msdownload') || mime.includes('x-sh')) {
        cb(new Error('File type not allowed (executable/script rejected)'), false);
    } else {
        cb(null, true);
    }
};

const upload = multer({
    storage: storage,
    limits: { fileSize: 10 * 1024 * 1024 }, // 10MB limit
    fileFilter: fileFilter
});

app.use('/uploads', express.static(uploadDir));

/**
 * REST Endpoint: Send notification with optional file attachment
 */
app.post('/notifications/send', upload.single('file'), async (req, res) => {
    try {
        const { coordinatorId, senderName, message } = req.body;
        if (!coordinatorId || !message) {
            return res.status(400).json({ error: 'coordinatorId and message are required' });
        }
        
        let attachmentUrl = null;
        let attachmentName = null;
        let attachmentMimeType = null;
        
        if (req.file) {
            const host = req.get('host');
            attachmentUrl = `${req.protocol}://${host}/uploads/${req.file.filename}`;
            attachmentName = req.file.originalname;
            attachmentMimeType = req.file.mimetype;
        }
        
        const notification = new Notification({
            coordinatorId,
            senderName: senderName || 'Coordinator',
            message,
            attachmentUrl,
            attachmentName,
            attachmentMimeType
        });
        
        await notification.save();
        
        const notificationPayload = {
            _id: notification._id.toString(),
            coordinatorId: notification.coordinatorId,
            senderName: notification.senderName,
            message: notification.message,
            attachmentUrl: notification.attachmentUrl,
            attachmentName: notification.attachmentName,
            attachmentMimeType: notification.attachmentMimeType,
            createdAt: notification.createdAt.toISOString()
        };
        
        io.to(coordinatorId).emit('new_notification', notificationPayload);
        
        res.json(notificationPayload);
    } catch (err) {
        console.error('Error sending notification:', err);
        res.status(500).json({ error: err.message || 'Internal Server Error' });
    }
});

/**
 * REST Endpoint: Fetch notifications for a coordinator group
 */
app.get('/notifications/:coordinatorId', async (req, res) => {
    try {
        const { coordinatorId } = req.params;
        const notifications = await Notification.find({ coordinatorId }).sort({ createdAt: -1 }).lean();
        
        const serialized = notifications.map(n => ({
            ...n,
            _id: n._id.toString(),
            createdAt: n.createdAt.toISOString()
        }));
        
        res.json(serialized);
    } catch (err) {
        console.error('Error fetching notifications:', err);
        res.status(500).json({ error: 'Internal Server Error' });
    }
});

// Initialize Socket.io logic
socketManager(io);

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
