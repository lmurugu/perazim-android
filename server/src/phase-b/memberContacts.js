'use strict';

// Compatibility exports for legacy callers. The private member register is
// intentionally unavailable to registration and ordinary application code.
// Membership is confirmed only through the authenticated administrator flow.
function loadMemberContacts() { return []; }
function classifyMemberPhone() { return null; }
function classifyRegistrationPhone() { return { status: 'REGISTERED', contact: null }; }

module.exports = { loadMemberContacts, classifyMemberPhone, classifyRegistrationPhone };
