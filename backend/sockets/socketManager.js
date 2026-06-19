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
            console.log(`[Socket] request_join received: name='${teacherName}', mobile='${mobileNumber}', coordId='${coordinatorId}', teacherGoogleId='${teacherGoogleId}'`);
            
            try {
                const newRequest = new JoinRequest({
                    teacherName,
                    teacherGoogleId,
                    mobileNumber,
                    coordinatorId
                });
                await newRequest.save();
                console.log(`[Socket] JoinRequest successfully saved to DB. ID: ${newRequest._id}`);

                // BUG FIX: Emit plain object with string ID instead of raw Mongoose doc.
                // This ensures the Android client receives a plain string for _id.
                io.to(coordinatorId).emit('new_join_request', {
                    _id: newRequest._id.toString(),
                    teacherName: newRequest.teacherName,
                    teacherGoogleId: newRequest.teacherGoogleId,
                    mobileNumber: newRequest.mobileNumber,
                    coordinatorId: newRequest.coordinatorId
                });
                console.log(`[Socket] Emitted new_join_request to room (coordinatorId): ${coordinatorId}`);
            } catch (err) {
                console.error('[Socket] Error in request_join saving/emitting request:', err);
            }
        });

        // Admin approves a request
        socket.on('approve_request', async (data) => {
            const { requestId, teacherGoogleId } = data;
            console.log(`[Socket] approve_request received for requestId='${requestId}', teacherGoogleId='${teacherGoogleId}'`);

            try {
                const request = await JoinRequest.findById(requestId);
                if (request) {
                    console.log(`[Socket] Found matching pending JoinRequest in DB: ${JSON.stringify(request)}`);
                    
                    // Update or insert teacher's record (upsert)
                    const updatedUser = await User.findOneAndUpdate(
                        { googleId: teacherGoogleId },
                        { 
                            name: request.teacherName,
                            mobileNumber: request.mobileNumber,
                            role: "Teacher", 
                            coordinatorId: request.coordinatorId 
                        },
                        { upsert: true, new: true }
                    );
                    console.log(`[Socket] MongoDB User collection upsert successful. User details: ${JSON.stringify(updatedUser)}`);

                    // Delete the request
                    await JoinRequest.findByIdAndDelete(requestId);
                    console.log(`[Socket] Pending JoinRequest deleted from DB: ${requestId}`);

                    // Notify teacher's socket room (using their own googleId as room)
                    io.to(teacherGoogleId).emit('join_request_resolved', { 
                        status: 'approved',
                        coordinatorId: request.coordinatorId 
                    });
                    console.log(`[Socket] Emitted join_request_resolved (approved) to room (teacherGoogleId): ${teacherGoogleId}`);
                } else {
                    console.warn(`[Socket] Warning: JoinRequest not found for ID ${requestId} during approval attempt.`);
                    // If request not found, emit denied with error to prevent teacher UI hanging
                    io.to(teacherGoogleId).emit('join_request_resolved', { 
                        status: 'denied', 
                        error: 'Request not found' 
                    });
                }
            } catch (err) {
                console.error('[Socket] Severe error in approve_request handler:', err);
            }
        });

        // Admin denies a request
        socket.on('deny_request', async (data) => {
            const { requestId, teacherGoogleId } = data;
            console.log(`[Socket] deny_request received for requestId='${requestId}', teacherGoogleId='${teacherGoogleId}'`);

            try {
                await JoinRequest.findByIdAndDelete(requestId);
                console.log(`[Socket] Pending JoinRequest deleted from DB: ${requestId}`);
                
                // Notify teacher
                io.to(teacherGoogleId).emit('join_request_resolved', { status: 'denied' });
                console.log(`[Socket] Emitted join_request_resolved (denied) to room (teacherGoogleId): ${teacherGoogleId}`);
            } catch (err) {
                console.error('[Socket] Error in deny_request handler:', err);
            }
        });

        socket.on('disconnect', () => {
            console.log(`User disconnected: ${socket.id}`);
        });
    });
};
