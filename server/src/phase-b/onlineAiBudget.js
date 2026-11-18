'use strict';

const { OnlineAiError, workerPseudonym } = require('./onlineAi');

const ACCOUNT_LIMIT = 3;
const budgetDate = new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Nairobi' });
const RESERVE = `
  INSERT INTO online_ai_daily_budget (budget_date, bucket, used)
  VALUES ($1, $2, 1)
  ON CONFLICT (budget_date, bucket)
  DO UPDATE SET used = online_ai_daily_budget.used + 1
    WHERE online_ai_daily_budget.used < $3
  RETURNING used
`;

async function reserveOnlineAiBudget(withTransaction, { userId, subjectKey, globalLimit, now = new Date() }) {
  if (!/^[1-9]\d{0,7}$/.test(globalLimit || '') || typeof subjectKey !== 'string' || subjectKey.length < 32) {
    throw new OnlineAiError(503, 'AI_BUDGET_UNCONFIGURED', 'The online service budget is unavailable.');
  }
  const parts = Object.fromEntries(budgetDate.formatToParts(now).map(({ type, value }) => [type, value]));
  const day = `${parts.year}-${parts.month}-${parts.day}`;
  const subject = workerPseudonym(userId, subjectKey);
  return withTransaction(async client => {
    // Acquire the global row first across all requests. Rejection rolls back both counters.
    const global = await client.query(RESERVE, [day, 'global', Number(globalLimit)]);
    if (!global.rows.length) throw new OnlineAiError(429, 'AI_SERVICE_LIMIT_REACHED', 'The online service limit has been reached.');
    const account = await client.query(RESERVE, [day, subject, ACCOUNT_LIMIT]);
    if (!account.rows.length) throw new OnlineAiError(429, 'AI_QUOTA_EXCEEDED', 'Your online question limit has been reached.');
    return ACCOUNT_LIMIT - account.rows[0].used;
  });
}

module.exports = { reserveOnlineAiBudget };
