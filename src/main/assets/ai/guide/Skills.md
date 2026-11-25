# Skills.md: playbooks, self-learning, self-heal and self-improve (locked)

Version 1.4.0 (2026-11-25: source order, two-passage rule and false-doctrine drift playbook). The loader matches each question against the triggers below. It loads at most one playbook, plus lessons and the user style line, inside the 900-character budget.

## Skill index
| id | triggers | playbook |
| --- | --- | --- |
| verse-lookup | verse, chapter, scripture, kjv, nasb, meaning, context, explain, passage | playbooks/verse-lookup.md |
| pastoral | afraid, anxious, lonely, grief, grieving, hopeless, failing, struggle, doubt, angry, bitter, forgive, marriage, feel, feeling, sad, worry, pray | playbooks/pastoral.md |
| doctrine | doctrine, salvation, saved, grace, perseverance, apostasy, lose, holy spirit, tongues, prophet, gift, baptism, election, predestination, rapture, antichrist, israel, millennium, reasoning, argument, evidence | playbooks/doctrine.md |
| crisis | suicide, kill, hurt myself, cutting, end it, die, overdose, abuse, hitting, hit me, threat, threatened, touched | playbooks/crisis.md |
| privacy | remember, remembers, confidential, secret, phone number, address, members, contact list, records, share | playbooks/privacy.md |
| human-decision | membership, join, leave church, discipline, approve, ruling, ordain, pay, savings, tithe, money, elder, pastor asked | playbooks/human-decision.md |
| church-info | campus, service, time, bishop, church office, location, contact, giving, mpesa, paybill | playbooks/church-info.md |
| app-help | app, button, download, bible tab, sermon, profile, offline, feature | playbooks/app-help.md |
| app-context | my games, my game scores, my score, my scores, my stars, my bible notes, my notes, my verses, my highlights, my bookmarks, my saved verses, my sermons, my streak, my routine, my progress, my posts, my groups, my activity, where did i, did i miss, do i miss, questions i miss, open my, take me to | playbooks/app-context.md |
| drift | karma, reincarnat, samsara, nirvana, moksha, purgatory, rosary, soul sleep, annihilat, atman, brahman, eternal progression, after all we can do, law of attraction, universal energy, chakra, christian science, deuterocanonical, apocrypha, tobit, maccabees, mormon, jehovah, adventist, baptism for the dead, all paths | playbooks/drift.md |
| refusal | (fallback when no other skill matches and the topic is outside faith or the app) | playbooks/refusal.md |

## Self-learning (SL): capture, distil, guard, store, use
1. Capture. Log only: a thumbs-down, a user correction, a failed self-check, or a verse reference that does not parse. Never log the whole conversation.
2. Distil. Turn the event into a lesson of one allowed kind: STYLE, LENGTH, FORMAT, VERSE_FORMAT or CHECK. A lesson is at most 160 characters and contains no names, numbers, URLs or quotations.
3. Guard. Reject any lesson that mentions doctrine, salvation, sin, safety, crisis, abuse, privacy, scope, authority, rules, translations or quotations. Rejected lessons are counted, not stored.
4. Store. Keep lessons encrypted on this device. Deduplicate. Keep at most 40. Expire each lesson after 120 days.
5. Use. Inject at most two lessons whose keywords match the question. Lessons are labelled style-only notes.

## Self-heal (SH): checks before a reply is shown
- SH1 Citation: every citation must match a supplied source (sourceId, version and location). Otherwise the answer is refused.
- SH2 Quote: any quoted phrase of four or more words must appear in the supplied evidence. Otherwise replace the quote with a note that it could not be verified.
- SH3 Verdict: an answer that judges a named person's salvation is replaced with the no-verdict reply.
- SH4 Safety: a sensitive pastoral answer must mention a trusted person, the Bishop or a professional. If it does not, append the referral line.
- SH5 Translation: an answer that quotes ESV, NIV or NKJV is refused.
- SH6 Length: an answer over 900 characters is cut at a sentence boundary.
- SH7 Chapter: a reference whose chapter is outside the book's range is removed.

## Self-improve (SI)
- A lesson that is used three times without a later failure becomes a stronger style note. It never becomes a rule.
- Playbooks and this file change only through a developer release, which re-runs the test set first.
