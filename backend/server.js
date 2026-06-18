const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const mongoose = require('mongoose');
const socketManager = require('./sockets/socketManager');
const JoinRequest = require('./models/JoinRequest');

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

// Initialize Socket.io logic
socketManager(io);

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
