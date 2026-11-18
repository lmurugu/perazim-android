/**
 * Perazim Mission Church — Community API Integration Test Suite
 * Tests real PostgreSQL integration for all Phase B Community features.
 */

const { assertDisposableTestDatabase } = require('./test_database_safety');
if (!process.env.DATABASE_URL) {
  throw new Error('Set DATABASE_URL to a disposable test database before running community_api.test.js');
}
assertDisposableTestDatabase(process.env.DATABASE_URL);
const crypto = require('crypto');
process.env.JWT_SECRET = process.env.JWT_SECRET || crypto.randomBytes(32).toString('hex');
process.env.PORT = '3008';

const assert = require('assert');
const http = require('http');
const { query } = require("../src/db");
const { app, startServer } = require('../src/index');
const { closeDb } = require('../src/db');
const { getCurrentNotice } = require('../src/phase-b/privacyNotice');

let server;
let baseUrl;
let registrationNotice;

async function registerOrLogin(phone, pin, name, campusId, role = "MEMBER") {
  const regRes = await request({
    hostname: "127.0.0.1",
    port: 3008,
    path: "/api/auth/register",
    method: "POST"
  }, { phone, pin, name, campus_id: campusId, role, ...registrationNotice });

  if (regRes.status === 201) {
    return { token: regRes.body.token, user: regRes.body.user };
  }

  // If 409, log in
  const loginRes = await request({
    hostname: "127.0.0.1",
    port: 3008,
    path: "/api/auth/login",
    method: "POST"
  }, { phone, pin });

  if (loginRes.status === 200) {
    return { token: loginRes.body.token, user: loginRes.body.user };
  }
  throw new Error("Failed to register or login user " + phone + ": " + JSON.stringify(loginRes.body));
}


function request(options, body = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let json = null;
        try {
          json = JSON.parse(data);
        } catch (e) {
          json = data;
        }
        resolve({ status: res.statusCode, headers: res.headers, body: json });
      });
    });
    req.on('error', reject);
    if (body) {
      if (typeof body === 'object') {
        req.setHeader('Content-Type', 'application/json');
        req.write(JSON.stringify(body));
      } else {
        req.write(body);
      }
    }
    req.end();
  });
}

async function runTests() {
  console.log('======================================================');
  console.log('🏛️ Starting Community API Integration Test Suite');
  console.log('======================================================\n');

  server = await startServer();
  baseUrl = 'http://127.0.0.1:3008';

  try {
    registrationNotice = await getCurrentNotice().then(notice => ({
      privacy_notice_version: notice.version,
      privacy_notice_hash: notice.content_hash,
      privacy_notice_acknowledged: true
    }));

    const phoneAdmin = `+2547${crypto.randomInt(0, 100000000).toString().padStart(8, '0')}`;
    const phoneMemberA = `+2547${crypto.randomInt(0, 100000000).toString().padStart(8, '0')}`;
    const phoneMemberB = `+2547${crypto.randomInt(0, 100000000).toString().padStart(8, '0')}`;

    // 1. Setup isolated synthetic admin and member accounts.

  const bcrypt = require("bcryptjs");
  const testPin = String(100000 + crypto.randomInt(900000));
  const testPinHash = bcrypt.hashSync(testPin, 10);
  await registerOrLogin(phoneAdmin, testPin, 'Synthetic Admin', 'campus_embu');
  await query("UPDATE users SET pin_hash = $1, role = 'ADMIN' WHERE phone = $2", [testPinHash, phoneAdmin]);

  console.log('--- Step 1: Registering Test Members ---');

    // Register synthetic admin.
    const { token: adminToken, user: adminUser } = await registerOrLogin(phoneAdmin, testPin, 'Synthetic Admin', 'campus_embu', 'ADMIN');
    console.log('✅ Synthetic admin registered (Token acquired)');

    // Register synthetic member A.
    const { token: memberAToken, user: memberAUser } = await registerOrLogin(phoneMemberA, testPin, 'Synthetic Member A', 'campus_embu');
    console.log('✅ Synthetic member A registered');

    // Register synthetic member B.
    const { token: memberBToken, user: memberBUser } = await registerOrLogin(phoneMemberB, testPin, 'Synthetic Member B', 'campus_nairobi');
    console.log('✅ Synthetic member B registered');

    // QR connection requests are server-authoritative across devices.
    const anonymousPending = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/connections/pending', method: 'GET'
    });
    assert.strictEqual(anonymousPending.status, 401, 'connection requests require authentication');
    const selfRequest = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/connections', method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, { peer_id: memberAUser.id });
    assert.strictEqual(selfRequest.status, 400, 'members cannot connect to themselves');
    const connectionRequest = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/connections', method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, { peer_id: memberBUser.id });
    assert.strictEqual(connectionRequest.status, 201, 'member A can request member B');
    assert.strictEqual(connectionRequest.body.connection.status, 'PENDING');
    assert.strictEqual(connectionRequest.body.connection.peer_name, 'Synthetic Member B', 'server supplies peer identity');
    const connectionId = connectionRequest.body.connection.id;
    const duplicateConnectionRequest = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/connections', method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, { peer_id: memberBUser.id });
    assert.strictEqual(duplicateConnectionRequest.status, 200, 'retrying a pending request is idempotent');
    assert.strictEqual(duplicateConnectionRequest.body.connection.id, connectionId);
    const incomingForB = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/connections/pending', method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert.strictEqual(incomingForB.status, 200);
    assert.strictEqual(incomingForB.body.requests.length, 1, 'only the recipient sees this incoming request');
    assert.strictEqual(incomingForB.body.requests[0].peer_id, memberAUser.id);
    const incomingForA = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/connections/pending', method: 'GET',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    });
    assert.strictEqual(incomingForA.body.requests.length, 0, 'requester does not see it as an incoming request');
    const requestNotification = await query(
      "SELECT COUNT(*)::int AS count FROM in_app_notifications WHERE user_id=$1 AND type='CONNECTION' AND target_id=$2",
      [memberBUser.id, connectionId]
    );
    assert.strictEqual(requestNotification.rows[0].count, 1, 'recipient gets one notification despite a retry');
    const unauthorizedResponse = await request({
      hostname: '127.0.0.1', port: 3008, path: `/api/community/connections/${connectionId}/respond`, method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, { decision: 'ACCEPT' });
    assert.strictEqual(unauthorizedResponse.status, 404, 'only the recipient may respond');
    const accepted = await request({
      hostname: '127.0.0.1', port: 3008, path: `/api/community/connections/${connectionId}/respond`, method: 'POST',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    }, { decision: 'ACCEPT' });
    assert.strictEqual(accepted.status, 200);
    assert.strictEqual(accepted.body.connection.status, 'ACCEPTED');
    for (const [token, peerId] of [[memberAToken, memberBUser.id], [memberBToken, memberAUser.id]]) {
      const acceptedConnections = await request({
        hostname: '127.0.0.1', port: 3008, path: '/api/community/connections', method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      assert.strictEqual(acceptedConnections.status, 200);
      assert(acceptedConnections.body.connections.some(connection => connection.peer_id === peerId),
        'both members see the accepted connection from shared server state');
    }
    console.log('✅ Server-backed QR connection request, notification, recipient-only approval, and shared accepted state verified');

    // 2. Profile Update (Bio and Avatar)
    console.log('\n--- Step 2: Member Profile Updates ---');
    const profUpdate = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/profile',
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      bio: 'Saved by grace. Serving in Embu sanctuary choir.',
      avatarUrl: 'default:1'
    });
    assert.strictEqual(profUpdate.status, 200);
    assert.strictEqual(profUpdate.body.user.bio, 'Saved by grace. Serving in Embu sanctuary choir.');
    assert.strictEqual(profUpdate.body.user.avatar_url, 'default:1');
    const rejectedExternalAvatar = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/profile', method: 'PUT',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, { avatarUrl: 'https://perazim.org/avatars/john.jpg' });
    assert.strictEqual(rejectedExternalAvatar.status, 400, 'profile pictures cannot reference an unowned remote URL');
    console.log('✅ Member Bio and Avatar updated successfully');

    // 3. Media Upload (Photo Attachment Validation)
    console.log('\n--- Step 3: Media Upload & Image Validation ---');
    // 1x1 valid PNG
    const png1x1Base64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
    const uploadRes = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/media/upload',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      base64Data: png1x1Base64,
      filename: 'testimony.png'
    });
    assert.strictEqual(uploadRes.status, 201);
    assert(uploadRes.body.mediaUrl.startsWith('/uploads/att_'));
    assert.strictEqual(uploadRes.body.format, 'png');
    const uploadedMediaUrl = uploadRes.body.mediaUrl;
    console.log(`✅ Compressed photo verified and saved: ${uploadedMediaUrl}`);

    const avatarUpdate = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/profile', method: 'PUT',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, { avatarUrl: uploadedMediaUrl });
    assert.strictEqual(avatarUpdate.status, 200);
    const directory = await request({
      hostname: '127.0.0.1', port: 3008, path: '/api/community/members', method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert(directory.body.members.some(member => member.id === memberAUser.id && member.avatar_url === uploadedMediaUrl));
    const directoryAvatar = await request({ hostname: '127.0.0.1', port: 3008, path: uploadedMediaUrl, method: 'GET' });
    assert.strictEqual(directoryAvatar.status, 404, 'directory avatars are not public to unauthenticated visitors');
    const authenticatedAvatar = await request({
      hostname: '127.0.0.1', port: 3008, path: uploadedMediaUrl, method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert.strictEqual(authenticatedAvatar.status, 200, 'authenticated directory members can load profile avatars');

    // Test rejection of non-image spoof
    const badUpload = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/media/upload',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      base64Data: Buffer.from('NOT_AN_IMAGE_FILE').toString('base64')
    });
    assert.strictEqual(badUpload.status, 400);
    console.log('✅ Malicious/non-image upload properly rejected (400)');

    // 4. Community Feed & Post Types
    console.log('\n--- Step 4: Community Feed & Posts ---');
    // General post
    const genPost = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/posts',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      type: 'GENERAL',
      title: 'Greetings saints!',
      content: 'Blessed Sunday to all who worship at Perazim Church today.',
      mediaUrl: uploadedMediaUrl
    });
    assert.strictEqual(genPost.status, 201);
    assert.strictEqual(genPost.body.post.type, 'GENERAL');
    const genPostId = genPost.body.post.id;
    console.log(`✅ Created GENERAL post with photo (${genPostId})`);

    // Praise post
    const praisePost = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/posts',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    }, {
      type: 'PRAISE',
      title: 'God provided for school fees!',
      content: 'Testifying of God’s provision after last Wednesday’s prayer meeting.'
    });
    assert.strictEqual(praisePost.status, 201);
    const praisePostId = praisePost.body.post.id;
    console.log(`✅ Created PRAISE report (${praisePostId})`);

    // Event post
    const eventDate = Date.now() + 86400000 * 3; // 3 days in future
    const eventPost = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/posts',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${adminToken}` }
    }, {
      type: 'EVENT',
      title: 'All-Night Kesha & Intercession',
      content: 'Join us this Friday at Embu Sanctuary for a night of breakthrough and prayer.',
      eventDate: eventDate,
      eventLocation: 'Embu Headquarters Main Sanctuary'
    });
    assert.strictEqual(eventPost.status, 201);
    const eventPostId = eventPost.body.post.id;
    console.log(`✅ Created EVENT post (${eventPostId})`);

    // Fetch feed filtered by PRAISE
    const praiseFeed = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/posts?type=PRAISE',
      method: 'GET'
    });
    assert.strictEqual(praiseFeed.status, 200);
    assert(praiseFeed.body.posts.some(p => p.id === praisePostId));
    console.log('✅ Filtered Feed by PRAISE successfully');

    // 5. Reactions: Exactly 1 Amen per Member
    console.log('\n--- Step 5: Exactly 1 Amen per Member ---');
    // John amens praise post
    const amen1 = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/posts/${praisePostId}/amen`,
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    });
    assert.strictEqual(amen1.status, 200);
    assert.strictEqual(amen1.body.amenCount, 1);
    assert.strictEqual(amen1.body.hasAmened, true);

    // John amens AGAIN: duplicate must NOT increment count!
    const amenDup = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/posts/${praisePostId}/amen`,
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    });
    assert.strictEqual(amenDup.status, 200);
    assert.strictEqual(amenDup.body.amenCount, 1); // Still 1!
    console.log('✅ Idempotent Amen verified: duplicate amen does not inflate count');

    // Synthetic admin amens praise post: count increments to 2
    const amenLevi = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/posts/${praisePostId}/amen`,
      method: 'POST',
      headers: { 'Authorization': `Bearer ${adminToken}` }
    });
    assert.strictEqual(amenLevi.body.amenCount, 2);
    console.log('✅ Second member Amen recorded: count updated to 2');

    // 6. Flat Comments & Notification Trigger
    console.log('\n--- Step 6: Flat Comments & In-App Alerts ---');
    const commentRes = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/posts/${praisePostId}/comments`,
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      content: 'Glory to God! Standing with you in thanksgiving, my sister.'
    });
    assert.strictEqual(commentRes.status, 201);
    assert.strictEqual(commentRes.body.comment.content, 'Glory to God! Standing with you in thanksgiving, my sister.');
    console.log('✅ Comment submitted');

    // Check that Synthetic Member B received in-app notification for the comment
    const maryNotifs = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/notifications',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert.strictEqual(maryNotifs.status, 200);
    assert(maryNotifs.body.notifications.some(n => n.type === 'COMMENT' && n.target_id === praisePostId));
    assert(maryNotifs.body.unreadCount >= 1);
    console.log('✅ In-App notification generated for post author');

    // 7. Event RSVPs
    console.log('\n--- Step 7: Event RSVPs ---');
    const rsvpJohn = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/posts/${eventPostId}/rsvp`,
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      status: 'ATTENDING'
    });
    assert.strictEqual(rsvpJohn.status, 200);
    assert.strictEqual(rsvpJohn.body.status, 'ATTENDING');
    assert.strictEqual(rsvpJohn.body.attendingCount, 1);
    assert.strictEqual(rsvpJohn.body.declinedCount, 0);

    const rsvpMary = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/posts/${eventPostId}/rsvp`,
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    }, {
      status: 'DECLINED'
    });
    assert.strictEqual(rsvpMary.body.attendingCount, 1);
    assert.strictEqual(rsvpMary.body.declinedCount, 1);
    console.log('✅ Event RSVPs recorded (1 Attending, 1 Declined)');

    // 8. Owner-Managed Groups
    console.log('\n--- Step 8: Owner-Managed Groups ---');
    const groupsList = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/groups',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${adminToken}` }
    });
    assert.strictEqual(groupsList.status, 200);
    assert(groupsList.body.groups.some(g => g.id === 'group_cell_embu'));
    assert(groupsList.body.groups.some(g => g.id === 'group_worship_team'));
    console.log('✅ Verified default seeded groups: Embu Cell & Worship Team');

    // Synthetic admin adds Synthetic Member B to Embu Cell
    const addMember = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/groups/group_cell_embu/members',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${adminToken}` }
    }, {
      userId: memberBUser.id,
      role: 'MEMBER'
    });
    assert.strictEqual(addMember.status, 200);

    // Verify Mary is in group roster
    const roster = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/groups/group_cell_embu/members',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert(roster.body.members.some(m => m.user_id === memberBUser.id));
    console.log('✅ Group member management verified');

    // Every signed-in member may own a private group.
    const badGroup = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/groups',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      name: 'John Private Study Group'
    });
    assert.strictEqual(badGroup.status, 201);
    const privateGroupId = badGroup.body.group.id;
    console.log('✅ Member-owned private group created');

    // 9. 1-to-1 Private Messaging
    console.log('\n--- Step 9: 1-to-1 Private Messaging ---');
    const sendMsg = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/messages',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      receiverId: memberBUser.id,
      content: 'Peace be with you Synthetic Member B. Are we singing in choir this Sunday?',
      mediaUrl: uploadedMediaUrl
    });
    assert.strictEqual(sendMsg.status, 201);
    assert.strictEqual(sendMsg.body.message.delivery_status, 'SENT');
    assert.strictEqual(sendMsg.body.message.delivered_at, null);
    const msgId = sendMsg.body.message.id;
    console.log(`✅ Sent direct message (${msgId}) with status SENT`);

    // Mary checks conversations
    const convs = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/conversations',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert.strictEqual(convs.status, 200);
    assert(convs.body.conversations.some(c => c.partner_id === memberAUser.id));
    assert(convs.body.conversations.some(c => parseInt(c.unread_count, 10) >= 1));
    console.log('✅ Conversation list verified with unread counter');

    // Mary fetches message thread with John
    const thread = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/messages/${memberAUser.id}`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert.strictEqual(thread.status, 200);
    assert(thread.body.messages.some(m => m.id === msgId));
    console.log('✅ Private message thread retrieved');

    // Mary acknowledges read
    const ackRes = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/messages/ack',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    }, {
      messageIds: [msgId],
      status: 'READ'
    });
    assert.strictEqual(ackRes.status, 200);
    assert.strictEqual(ackRes.body.updated, 1);
    console.log('✅ Message marked as READ');

    // Scope and retry regressions: outsiders cannot enumerate private content/media.
    const api = (method, route, token, body) => request({hostname:'127.0.0.1',port:3008,path:route,method,
      headers:token?{Authorization:`Bearer ${token}`} : {}},body);
    const privateUpload = await api('POST','/api/community/media/upload',memberAToken,{base64Data:png1x1Base64});
    assert.strictEqual(privateUpload.status,201);
    const privateUrl=privateUpload.body.mediaUrl;
    const privatePost = await api('POST','/api/community/posts',memberAToken,{id:`post_private_${Date.now()}`,title:'Private group',content:'Members only',groupId:privateGroupId,mediaUrl:privateUrl});
    assert.strictEqual(privatePost.status,201);
    const privateId=privatePost.body.post.id;
    assert.strictEqual((await api('GET',`/api/community/posts?groupId=${privateGroupId}`,memberBToken)).status,403);
    assert.strictEqual((await api('GET',`/api/community/groups/${privateGroupId}/members`,memberBToken)).status,403);
    assert.strictEqual((await api('GET',`/api/community/posts/${privateId}/comments`,memberBToken)).status,404);
    assert.strictEqual((await api('POST',`/api/community/posts/${privateId}/amen`,memberBToken,{})).status,404);
    assert.strictEqual((await api('GET',privateUrl,null)).status,404);
    assert.strictEqual((await api('GET',privateUrl,memberBToken)).status,404);
    assert.strictEqual((await api('GET',privateUrl,memberAToken)).status,200);
    assert.strictEqual((await api('POST','/api/community/posts',memberBToken,{title:'Stolen upload',content:'Not mine',mediaUrl:privateUrl})).status,400);
    const publicPosts=await api('GET','/api/community/posts?limit=100',null);
    assert(!publicPosts.body.posts.some(p=>p.id===privateId));
    assert.strictEqual((await api('POST',`/api/community/groups/${privateGroupId}/members`,memberAToken,{userId:memberBUser.id})).status,200);
    assert.strictEqual((await api('GET',privateUrl,memberBToken)).status,200);
    const scheduled=await api('POST','/api/community/classes',memberAToken,{id:`class_test_${Date.now()}`,groupId:privateGroupId,title:'Bible study',startsAt:Date.now()+3600000,durationMinutes:60});
    assert.strictEqual(scheduled.status,201);
    const classId=scheduled.body.class.id;
    assert((await api('GET','/api/community/classes',memberBToken)).body.classes.some(c=>c.id===classId));
    assert.strictEqual((await api('DELETE',`/api/community/classes/${classId}`,memberBToken)).status,403);
    assert.strictEqual((await api('DELETE',`/api/community/groups/${privateGroupId}/members/${memberBUser.id}`,memberAToken)).status,200);
    assert.strictEqual((await api('GET',privateUrl,memberBToken)).status,404);
    assert(!(await api('GET','/api/community/classes',memberBToken)).body.classes.some(c=>c.id===classId));
    assert.strictEqual((await api('DELETE',`/api/community/classes/${classId}`,memberAToken)).status,200);
    const retryPayload={id:`msg_retry_${Date.now()}`,receiverId:memberBUser.id,content:'Retry once'};
    const firstSend=await api('POST','/api/community/messages',memberAToken,retryPayload);
    const retrySend=await api('POST','/api/community/messages',memberAToken,retryPayload);
    assert.strictEqual(firstSend.status,201);assert.strictEqual(retrySend.status,200);
    assert.strictEqual(firstSend.body.message.id,retrySend.body.message.id);
    const outsiderThread=await api('GET',`/api/community/messages/${memberAUser.id}`,adminToken);
    assert(!outsiderThread.body.messages.some(m=>m.id===retryPayload.id));
    const outsiderAck=await api('POST','/api/community/messages/ack',adminToken,{messageIds:[retryPayload.id],status:'READ'});
    assert.strictEqual(outsiderAck.body.updated,0);
    const senderThread=await api('GET',`/api/community/messages/${memberBUser.id}`,memberAToken);
    assert.strictEqual(senderThread.body.messages.find(m=>m.id===msgId).delivery_status,'READ');
    console.log('✅ Private groups, scoped media, classes, membership revocation, and idempotent private messaging verified');

    await query('DELETE FROM user_challenge_completions WHERE user_id = $1', [memberAUser.id]);
    // 10. Daily Bible Challenge & Zero Reward Farming
    console.log('\n--- Step 10: Daily Bible Challenge ---');
    const todayChal = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/challenge/today',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    });
    assert.strictEqual(todayChal.status, 200);
    assert(todayChal.body.question);
    assert(todayChal.body.hint);
    assert.strictEqual(todayChal.body.isCompleted, false);
    assert.strictEqual(todayChal.body.answer, undefined, 'Answer must NEVER be leaked in API response');
    console.log(`✅ Today\'s Challenge fetched (${todayChal.body.date}): Answer safely hidden`);

    // Submit incorrect answer
    const wrongAns = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/challenge/submit',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      answer: 'completely_wrong_answer'
    });
    assert.strictEqual(wrongAns.status, 200);
    assert.strictEqual(wrongAns.body.success, false);
    console.log('✅ Incorrect answer recognized gracefully');

    // Fetch stored challenge answer from DB to test correct submission

    const chalDb = await query('SELECT answer FROM daily_challenges WHERE date = $1', [todayChal.body.date]);
    const correctAnswer = chalDb.rows[0].answer;

    // Submit correct answer
    const correctAns = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/challenge/submit',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      answer: correctAnswer
    });
    assert.strictEqual(correctAns.status, 200);
    assert.strictEqual(correctAns.body.success, true);
    assert.strictEqual(correctAns.body.xpAwarded, 20);
    console.log('✅ Correct answer submitted: +20 XP awarded');

    // Replay submission on same day (Zero reward farming)
    const replayAns = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/challenge/submit',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberAToken}` }
    }, {
      answer: correctAnswer
    });
    assert.strictEqual(replayAns.status, 200);
    assert.strictEqual(replayAns.body.alreadyCompleted, true);
    assert.strictEqual(replayAns.body.xpAwarded, 0);
    console.log('✅ Zero reward farming verified: 0 additional XP on replay');

    // 11. Notifications: Mark All Read
    console.log('\n--- Step 11: Notification Read State ---');
    const readAllNotifs = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/notifications/read',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    }, {});
    assert.strictEqual(readAllNotifs.status, 200);

    const checkMaryNotifs = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/notifications',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${memberBToken}` }
    });
    assert.strictEqual(checkMaryNotifs.body.unreadCount, 0);
    console.log('✅ Notifications successfully marked as read');

    console.log('\n======================================================');
    console.log('🎉 ALL 11 COMMUNITY API INTEGRATION TESTS PASSED!');
    console.log('======================================================\n');
  } finally {
    if (server) {
      await new Promise(resolve => server.close(resolve));
    }
    await closeDb();
  }
}

runTests().catch(err => {
  console.error('❌ Community API Test Failure:', err);
  process.exit(1);
});
