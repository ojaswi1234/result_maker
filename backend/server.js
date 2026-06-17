const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const mongoose = require('mongoose');
const socketManager = require('./sockets/socketManager');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

const MONGO_URI = "mongodb+srv://mockAdmin:mockPassword123@cluster0.mock.mongodb.net/school_management?retryWrites=true&w=majority";

mongoose.connect(MONGO_URI)
    .then(() => console.log('Connected to MongoDB successfully'))
    .catch(err => console.error('MongoDB connection error:', err));

app.use(express.json());

// Initialize Socket.io logic
socketManager(io);

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
