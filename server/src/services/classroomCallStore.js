const crypto = require('crypto');

class CallError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

function participantLimit(value) {
  if (value === undefined || value === '') return 4;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 2 || parsed > 4) throw new Error('CLASSROOM_MAX_PARTICIPANTS must be 2, 3, or 4');
  return parsed;
}

function signalPayload(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new CallError(400, 'Invalid signal');
  if (input.type === 'offer' || input.type === 'answer') {
    if (typeof input.sdp !== 'string' || input.sdp.length < 5 || Buffer.byteLength(input.sdp) > 65536) throw new CallError(400, 'Invalid session description');
    return { type: input.type, sdp: input.sdp };
  }
  if (input.type === 'ice') {
    const c = input.candidate;
    if (!c || typeof c.candidate !== 'string' || c.candidate.length > 4096 ||
      (c.sdpMid != null && (typeof c.sdpMid !== 'string' || c.sdpMid.length > 32)) ||
      (c.sdpMLineIndex != null && (!Number.isInteger(c.sdpMLineIndex) || c.sdpMLineIndex < 0 || c.sdpMLineIndex > 16)) ||
      (c.usernameFragment != null && (typeof c.usernameFragment !== 'string' || c.usernameFragment.length > 128))) throw new CallError(400, 'Invalid ICE candidate');
    return { type: 'ice', candidate: { candidate: c.candidate, sdpMid: c.sdpMid ?? null, sdpMLineIndex: c.sdpMLineIndex ?? null, ...(c.usernameFragment ? { usernameFragment: c.usernameFragment } : {}) } };
  }
  throw new CallError(400, 'Unsupported signal type');
}

/** Ephemeral signaling only. Media flows directly between at most four local peers. */
class ClassroomCallStore {
  constructor({ now = Date.now, maxParticipants = 4, ttl = 45000, maxRooms = 100 } = {}) {
    this.now = now;
    this.limit = participantLimit(String(maxParticipants));
    this.ttl = ttl;
    this.maxRooms = maxRooms;
    this.rooms = new Map();
  }

  prune() {
    const now = this.now();
    for (const [id, room] of this.rooms) {
      for (const [sid, member] of room.members) if (now - member.lastSeen > this.ttl) room.members.delete(sid);
      room.events = room.events.filter(e => now - e.created < 60000 && room.members.has(e.target));
      if (!room.members.size) this.rooms.delete(id);
    }
  }

  roster(room) {
    return [...room.members.values()].map(m => ({ sessionId: m.id, userId: m.userId, displayName: m.name }));
  }

  join(classId, user, joinKey) {
    this.prune();
    if (typeof joinKey !== 'string' || !/^[a-zA-Z0-9_-]{16,80}$/.test(joinKey)) throw new CallError(400, 'Invalid join request');
    let room = this.rooms.get(classId);
    if (!room) {
      if (this.rooms.size >= this.maxRooms) throw new CallError(503, 'Classrooms are busy. Try again shortly.');
      room = { members: new Map(), events: [], sequence: 0 };
      this.rooms.set(classId, room);
    }
    const existing = [...room.members.values()].find(m => m.userId === user.id);
    if (existing && existing.joinKey !== joinKey) throw new CallError(409, 'You are already in this classroom on another session. Leave it or wait 45 seconds.');
    if (existing) {
      existing.lastSeen = this.now();
      return { sessionId: existing.id, participants: this.roster(room), cursor: existing.joinCursor, maxParticipants: this.limit };
    }
    if (room.members.size >= this.limit) throw new CallError(409, `This local classroom is full (${this.limit} participants).`);
    const id = crypto.randomBytes(18).toString('hex');
    room.members.set(id, { id, userId: user.id, name: String(user.name || 'Member').slice(0, 128), joinKey, lastSeen: this.now(), joinCursor: room.sequence, rateStart: this.now(), rateCount: 0 });
    return { sessionId: id, participants: this.roster(room), cursor: room.sequence, maxParticipants: this.limit };
  }

  owned(classId, sessionId, userId) {
    this.prune();
    const room = this.rooms.get(classId);
    const member = room && room.members.get(sessionId);
    if (!member) throw new CallError(410, 'Classroom session ended. Join again.');
    if (member.userId !== userId) throw new CallError(403, 'This classroom session belongs to another member');
    return { room, member };
  }

  poll(classId, sessionId, userId, after) {
    const { room, member } = this.owned(classId, sessionId, userId);
    if (!Number.isSafeInteger(after) || after < 0 || after > room.sequence) throw new CallError(400, 'Invalid signal cursor');
    member.lastSeen = this.now();
    room.events = room.events.filter(e => e.target !== sessionId || e.id > after);
    return { participants: this.roster(room), events: room.events.filter(e => e.target === sessionId && e.id > after).map(({ id, from, payload }) => ({ id, from, payload })), cursor: room.sequence };
  }

  signal(classId, sessionId, userId, target, input) {
    const { room, member } = this.owned(classId, sessionId, userId);
    if (target === sessionId || !room.members.has(target)) throw new CallError(404, 'The other participant has left');
    const payload = signalPayload(input);
    const now = this.now();
    if (now - member.rateStart >= 10000) { member.rateStart = now; member.rateCount = 0; }
    if (++member.rateCount > 180) throw new CallError(429, 'Too many classroom signals');
    if (room.events.filter(e => e.target === target).length >= 256) throw new CallError(429, 'The other participant is not receiving signals');
    member.lastSeen = now;
    room.events.push({ id: ++room.sequence, target, from: sessionId, payload, created: now });
  }

  leave(classId, sessionId, userId) {
    const { room } = this.owned(classId, sessionId, userId);
    room.members.delete(sessionId);
    room.events = room.events.filter(e => e.target !== sessionId && e.from !== sessionId);
    if (!room.members.size) this.rooms.delete(classId);
  }
}

module.exports = { ClassroomCallStore, CallError, participantLimit, signalPayload };
