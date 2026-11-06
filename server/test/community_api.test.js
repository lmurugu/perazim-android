/**
 * Perazim Mission Church — Community API Integration Test Suite
 * Tests real PostgreSQL integration for all Phase B Community features.
 */

process.env.DATABASE_URL = 'postgres://perazim_test:test_secret_pass_2026@127.0.0.1:5433/perazim_test';
process.env.JWT_SECRET = 'community-test-jwt-secret-perazim-2026-secure';
process.env.PORT = '3008';

const assert = require('assert');
const http = require('http');
const { query } = require("../src/db");
const { app, startServer } = require('../src/index');
const { closeDb } = require('../src/db');

let server;
let baseUrl;

async function registerOrLogin(phone, pin, name, campusId, role = "MEMBER") {
  const regRes = await request({
    hostname: "127.0.0.1",
    port: 3008,
    path: "/api/auth/register",
    method: "POST"
  }, { phone, pin, name, campusId, role });

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
    // 1. Setup Test Accounts (Levi the Admin, Brother John, Sister Mary)

  const bcrypt = require("bcryptjs");
  const hash1234 = bcrypt.hashSync("1234", 10);
  await registerOrLogin('0715440482', '1234', 'Pastor Levi', 'campus_embu');
  await query("UPDATE users SET pin_hash = $1, role = 'ADMIN' WHERE phone = '+254715440482' OR phone = '0715440482'", [hash1234]);
  await query("UPDATE users SET pin_hash = $1 WHERE phone = '+254711111111' OR phone = '0711111111'", [hash1234]);
  await query("UPDATE users SET pin_hash = $1 WHERE phone = '+254722222222' OR phone = '0722222222'", [hash1234]);

  console.log('--- Step 1: Registering Test Members ---');

    // Register Levi (Admin)
    const { token: leviToken, user: leviUser } = await registerOrLogin('0715440482', '1234', 'Pastor Levi', 'campus_embu', 'ADMIN');
    console.log('✅ Admin Levi registered (Token acquired)');

    // Register Member 1 (Brother John)
    const { token: johnToken, user: johnUser } = await registerOrLogin('0711111111', '1234', 'Brother John', 'campus_embu');
    console.log('✅ Brother John registered');

    // Register Member 2 (Sister Mary)
    const { token: maryToken, user: maryUser } = await registerOrLogin('0722222222', '1234', 'Sister Mary', 'campus_nairobi');
    console.log('✅ Sister Mary registered');

    // 2. Profile Update (Bio and Avatar)
    console.log('\n--- Step 2: Member Profile Updates ---');
    const profUpdate = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/profile',
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${johnToken}` }
    }, {
      bio: 'Saved by grace. Serving in Embu sanctuary choir.',
      avatarUrl: 'https://perazim.org/avatars/john.jpg'
    });
    assert.strictEqual(profUpdate.status, 200);
    assert.strictEqual(profUpdate.body.user.bio, 'Saved by grace. Serving in Embu sanctuary choir.');
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
    }, {
      base64Data: png1x1Base64,
      filename: 'testimony.png'
    });
    assert.strictEqual(uploadRes.status, 201);
    assert(uploadRes.body.mediaUrl.startsWith('/uploads/att_'));
    assert.strictEqual(uploadRes.body.format, 'png');
    const uploadedMediaUrl = uploadRes.body.mediaUrl;
    console.log(`✅ Compressed photo verified and saved: ${uploadedMediaUrl}`);

    // Test rejection of non-image spoof
    const badUpload = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/media/upload',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${maryToken}` }
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
      headers: { 'Authorization': `Bearer ${leviToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
    });
    assert.strictEqual(amenDup.status, 200);
    assert.strictEqual(amenDup.body.amenCount, 1); // Still 1!
    console.log('✅ Idempotent Amen verified: duplicate amen does not inflate count');

    // Levi amens praise post: count increments to 2
    const amenLevi = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/posts/${praisePostId}/amen`,
      method: 'POST',
      headers: { 'Authorization': `Bearer ${leviToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
    }, {
      content: 'Glory to God! Standing with you in thanksgiving, my sister.'
    });
    assert.strictEqual(commentRes.status, 201);
    assert.strictEqual(commentRes.body.comment.content, 'Glory to God! Standing with you in thanksgiving, my sister.');
    console.log('✅ Comment submitted');

    // Check that Sister Mary received in-app notification for the comment
    const maryNotifs = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/notifications',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${maryToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${maryToken}` }
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
      headers: { 'Authorization': `Bearer ${leviToken}` }
    });
    assert.strictEqual(groupsList.status, 200);
    assert(groupsList.body.groups.some(g => g.id === 'group_cell_embu'));
    assert(groupsList.body.groups.some(g => g.id === 'group_worship_team'));
    console.log('✅ Verified default seeded groups: Embu Cell & Worship Team');

    // Levi adds Sister Mary to Embu Cell
    const addMember = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/groups/group_cell_embu/members',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${leviToken}` }
    }, {
      userId: maryUser.id,
      role: 'MEMBER'
    });
    assert.strictEqual(addMember.status, 200);

    // Verify Mary is in group roster
    const roster = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/groups/group_cell_embu/members',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${maryToken}` }
    });
    assert(roster.body.members.some(m => m.user_id === maryUser.id));
    console.log('✅ Group member management verified');

    // Non-admin attempting to create group is rejected with 403
    const badGroup = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/groups',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${johnToken}` }
    }, {
      name: 'Unauthorized Rogue Group'
    });
    assert.strictEqual(badGroup.status, 403);
    console.log('✅ Non-admin group creation rejected with 403 Forbidden');

    // 9. 1-to-1 Private Messaging
    console.log('\n--- Step 9: 1-to-1 Private Messaging ---');
    const sendMsg = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/messages',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${johnToken}` }
    }, {
      receiverId: maryUser.id,
      content: 'Peace be with you Sister Mary. Are we singing in choir this Sunday?',
      mediaUrl: uploadedMediaUrl
    });
    assert.strictEqual(sendMsg.status, 201);
    assert.strictEqual(sendMsg.body.message.delivery_status, 'DELIVERED');
    const msgId = sendMsg.body.message.id;
    console.log(`✅ Sent direct message (${msgId}) with status DELIVERED`);

    // Mary checks conversations
    const convs = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/conversations',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${maryToken}` }
    });
    assert.strictEqual(convs.status, 200);
    assert(convs.body.conversations.some(c => c.partner_id === johnUser.id));
    assert(convs.body.conversations.some(c => parseInt(c.unread_count, 10) >= 1));
    console.log('✅ Conversation list verified with unread counter');

    // Mary fetches message thread with John
    const thread = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: `/api/community/messages/${johnUser.id}`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${maryToken}` }
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
      headers: { 'Authorization': `Bearer ${maryToken}` }
    }, {
      messageIds: [msgId],
      status: 'READ'
    });
    assert.strictEqual(ackRes.status, 200);
    assert.strictEqual(ackRes.body.updated, 1);
    console.log('✅ Message marked as READ');

    // 10. Daily Bible Challenge & Zero Reward Farming
    console.log('\n--- Step 10: Daily Bible Challenge ---');
    const todayChal = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/challenge/today',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${johnToken}` }
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
      headers: { 'Authorization': `Bearer ${maryToken}` }
    }, {});
    assert.strictEqual(readAllNotifs.status, 200);

    const checkMaryNotifs = await request({
      hostname: '127.0.0.1',
      port: 3008,
      path: '/api/community/notifications',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${maryToken}` }
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
