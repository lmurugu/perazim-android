<!-- Faithful text rendition of the supplied working draft. Draft status and placeholders are preserved. -->


PERAZIM MISSIONS CHURCH INTERNATIONAL
# PMCI Data Protection and Privacy Policy

KENYA  |  VERSION 0.1  |  DRAFT FOR REVIEW

## 1. Status and application

DRAFT FOR REVIEW. This policy describes how Perazim Missions Church International (PMCI) proposes to collect, use, store, disclose, retain and protect personal data in its church administration and digital services, including the Perazim application. It applies to members, visitors, applicants, parents/guardians, volunteers, employees, ministry workers and other people whose information PMCI processes. This policy becomes effective only after formal approval and publication.

## 2. Who is responsible

Data controller: [Insert exact registered legal name of PMCI]. Address: [insert]. Privacy contact: [insert dedicated email and telephone]. Responsible officer: [insert title/name]. ODPC registration: [insert registration number or status]. PMCI must verify its registration obligations and complete any required registration before operational processing.

## 3. Personal data PMCI may process

Depending on the person's relationship with PMCI and the service used, information may include names, telephone numbers, email and postal addresses, account identifiers, authentication and security records, membership status, membership history, ministry or participation information, communications, attendance information where separately approved, and information about a parent/guardian where necessary. The application should not collect information merely because it is technically possible. Sensitive personal data, including information that reveals religious affiliation, receives heightened protection. Children's data requires additional safeguards.

## 4. Purposes

PMCI may process personal data for specified church purposes, including maintaining membership and contact records; communicating church notices; creating and securing application accounts; administering ministry and church services; receiving and reviewing membership claims; confirming membership through an authorised human decision; responding to enquiries and exercising data-subject rights; maintaining security and preventing fraud; meeting legal obligations; and maintaining necessary governance, financial or safeguarding records. Each purpose must have a documented lawful basis and be limited to necessary data.

## 5. Lawful basis

PMCI will identify and document an appropriate lawful basis under section 30 of the Data Protection Act, 2019 for each processing purpose. Depending on the circumstances, a basis may include consent, necessity for a contract or requested steps, legal obligation, protection of vital interests, public interest where applicable, or legitimate interests subject to the statutory test. For sensitive personal data, PMCI will also assess sections 44–45, including whether the conditions for a qualifying non-profit religious body's legitimate activities are met. PMCI does not claim that this policy itself creates consent or establishes eligibility for a statutory exception. Where consent is relied upon, it will be specific, informed, freely given, demonstrable and withdrawable.

## 6. Membership register and verification

The software provides restricted server-side import, claim review and verification workflows, but this checkout does not establish that a current, church-approved register has been imported or validated. Do not treat test fixtures as church records. An applicant's phone number, PIN, registration or name match does not automatically create VERIFIED_MEMBER status. Only an authorised administrator can confirm or reject a claim after review; the workflow records decisions and gives the applicant a neutral receipt.

## 7. Access and confidentiality

Access is limited by authentication, explicit permissions and need. The signed-in member directory is searchable and returns member names, account identifiers and avatar references; it does not return telephone numbers. Authorised administrators may access specified claim and register functions after step-up authentication. Ordinary users cannot query register candidates or inspect private membership records. The member register is not packaged in Android assets or intentionally sent to AI. No live approved register is verified in this development checkout. Staff confidentiality procedures remain an institutional responsibility.

## 8. Service providers and disclosures

The online AI option sends user-entered questions, bounded conversation history, a device identifier and limited app context through the configured relay for Google Gemini processing. The on-device Qwen model is downloaded from its configured source and runs locally after download. Hosting, relay, model-download, backup and other provider identities, locations, retention settings and agreements have not all been independently verified and must be completed before publication. The app does not use an SMS/OTP provider. PMCI does not sell personal information.

## 9. Security safeguards

Verified in this checkout: protected Android API calls require HTTPS in ordinary build types; a validation build has explicit loopback-only cleartext exceptions; PINs are stored server-side as password hashes; administrator step-up uses PIN and TOTP with expiring/revocable privileged sessions; and selected audit records are append-only against ordinary SQL roles. The session token is stored with Android Keystore-backed AES-GCM. The Room database is not encrypted by the app. Android backup is enabled and excludes the session-token preference files, but does not exclude the Room database or all other app data. Database encryption at rest, backup-provider protections/expiry, production TLS, and independent operational controls are not verified. PostgreSQL owners and superusers can alter database protections. Do not interpret this draft as proof of encryption at rest or a deployed secure service.

## 10. Retention and deletion

No approved, operational retention schedule or documented backup-expiry cycle is present in this checkout. Governance code supports synthetic retention dry-runs and records review/disposal decisions, but it does not automatically dispose of production records or prove backups expire. PMCI must approve retention periods, legal holds, disposal authority, backup controls and recovery evidence before making operational retention promises.

## 11. Data-subject rights

The API currently accepts authenticated ACCESS, CORRECTION and ERASURE request intake and returns a receipt/status; intake does not fulfil the request. The app has no implemented objection, restriction or portability workflow. Consent choices can be recorded only for configured purposes, and there is no complete optional-consent UI. Request validation, identity checks, review, fulfilment, response deadlines and escalation require an operational process. Contact [privacy contact]. No request concerning one person entitles them to another person's data.

## 12. Children

The current app does not perform an age check and does not provide a parent/guardian flow. It therefore does not implement the safeguards described in this proposed section. PMCI must decide whether children may use the app and implement and test required controls before publication or child use.

## 13. Automated decisions

PMCI will not use a contact-list match, phone number, algorithm or AI model as the sole basis for granting verified church membership. Membership confirmation requires authorised human review. Any future automated decision-making with legal or similarly significant effects requires a separate legal assessment and appropriate safeguards.

## 14. Personal-data incidents

Suspected loss, unauthorised access, disclosure, alteration or destruction must be reported immediately through [incident contact]. PMCI will investigate, contain, preserve evidence, assess risk and make notifications to the ODPC and affected individuals where required by section 43 and applicable regulations. Staff must not conceal incidents or independently contact affected persons unless authorised under the response procedure.

## 15. Complaints and contact

Questions, requests or complaints may be sent to [privacy email], [telephone] or [postal/physical address]. An individual may also complain to the Office of the Data Protection Commissioner, Kenya: https://www.odpc.go.ke/.

## 16. Changes

PMCI will review this policy at least annually and when purposes, systems, providers, laws or risks materially change. Material changes will be communicated appropriately. The policy version and effective date will be displayed here: Version [x], effective [date], last reviewed [date].

## 17. Legal framework

This policy is intended to operate consistently with the Constitution of Kenya, the Data Protection Act, 2019, the Data Protection (General) Regulations, 2021, the Data Protection (Registration of Data Controllers and Data Processors) Regulations, 2021 and other applicable law. It must be reviewed against current official instruments and PMCI's actual circumstances before approval.
