-- Approver signature over each institutional source approval (S2-06 item 3).
-- The approver signs offline with a key the server never holds. The server stores only the signature and verifies it
-- before it approves a source or includes one in a package.
ALTER TABLE ai_knowledge_sources ADD COLUMN IF NOT EXISTS approval_signature TEXT;
