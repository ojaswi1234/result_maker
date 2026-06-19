require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const mongoose = require('mongoose');
const socketManager = require('./sockets/socketManager');
const JoinRequest = require('./models/JoinRequest');
const User = require('./models/User');

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

        const updateData = {};
        if (name !== undefined) updateData.name = name;
        if (mobileNumber !== undefined) updateData.mobileNumber = mobileNumber;
        if (role !== undefined) updateData.role = role;
        if (coordinatorId !== undefined) updateData.coordinatorId = coordinatorId;

        const user = await User.findOneAndUpdate(
            { googleId },
            updateData,
            { upsert: true, new: true }
        );
        res.json(user);
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

// Initialize Socket.io logic
socketManager(io);

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
