const User = require('../models/User');
const JoinRequest = require('../models/JoinRequest');

module.exports = (io) => {
    io.on('connection', (socket) => {
        console.log(`User connected: ${socket.id}`);

        // Join room based on coordinatorId for targeted notifications
        socket.on('join_room', (coordinatorId) => {
            socket.join(coordinatorId);
            console.log(`Socket ${socket.id} joined room: ${coordinatorId}`);
        });

        // Teacher requests to join an Admin's hierarchy
        socket.on('request_join', async (data) => {
            const { teacherName, mobileNumber, coordinatorId, teacherGoogleId } = data;
            
            try {
                const newRequest = new JoinRequest({
                    teacherName,
                    teacherGoogleId,
                    mobileNumber,
                    coordinatorId
                });
                await newRequest.save();

                // BUG FIX: Emit plain object with string ID instead of raw Mongoose doc.
                // This ensures the Android client receives a plain string for _id.
                io.to(coordinatorId).emit('new_join_request', {
                    _id: newRequest._id.toString(),
                    teacherName: newRequest.teacherName,
                    teacherGoogleId: newRequest.teacherGoogleId,
                    mobileNumber: newRequest.mobileNumber,
                    coordinatorId: newRequest.coordinatorId
                });
            } catch (err) {
                console.error('Error in request_join:', err);
            }
        });

        // Admin approves a request
        socket.on('approve_request', async (data) => {
            const { requestId, teacherGoogleId } = data;

            try {
                const request = await JoinRequest.findById(requestId);
                if (request) {
                    // Update or insert teacher's record (upsert)
                    await User.findOneAndUpdate(
                        { googleId: teacherGoogleId },
                        { 
                            name: request.teacherName,
                            mobileNumber: request.mobileNumber,
                            role: "Teacher", 
                            coordinatorId: request.coordinatorId 
                        },
                        { upsert: true, new: true }
                    );

                    // Delete the request
                    await JoinRequest.findByIdAndDelete(requestId);

                    // Notify teacher's socket room (using their own googleId as room)
                    io.to(teacherGoogleId).emit('join_request_resolved', { 
                        status: 'approved',
                        coordinatorId: request.coordinatorId 
                    });
                } else {
                    // If request not found, emit denied with error to prevent teacher UI hanging
                    io.to(teacherGoogleId).emit('join_request_resolved', { 
                        status: 'denied', 
                        error: 'Request not found' 
                    });
                }
            } catch (err) {
                console.error('Error in approve_request:', err);
            }
        });

        // Admin denies a request
        socket.on('deny_request', async (data) => {
            const { requestId, teacherGoogleId } = data;

            try {
                await JoinRequest.findByIdAndDelete(requestId);
                
                // Notify teacher
                io.to(teacherGoogleId).emit('join_request_resolved', { status: 'denied' });
            } catch (err) {
                console.error('Error in deny_request:', err);
            }
        });

        socket.on('disconnect', () => {
            console.log(`User disconnected: ${socket.id}`);
        });
    });
};
