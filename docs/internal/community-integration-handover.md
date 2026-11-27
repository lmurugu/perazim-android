# Community and Bible Search integration handover

Work is in /home/murugu/Android/app on codex/community-integration. It builds on the committed Games, Daily Reading, and Assistant work in codex/assistant-dictation. Claude's Bible Search overview from 1bcf7a8 and the sermons fallback from 878039e were brought into this branch.

The Community feed has a pinned-event area, Inbox and new-post actions, member group shortcuts, and post filters. A pinned event appears only when the server returns a current pinned event. The Groups, Discover, and prayer-audience designs and the requested church membership and administrative rules still need implementation and end-to-end verification.

The earlier version 35 APK was built with the old private Tailscale origin. The previously documented Cloudflare quick tunnel and health gateway are inactive. That tunnel served health only and was not verified for community, sign-in, or messaging. A working HTTPS origin serving the required API is needed before a Cloudflare-connected build can be validated. The phone's saved server setting takes precedence over the APK's built-in origin.

Preserve account data and current memberships. Use synthetic accounts for access-control checks. Verify nonmember denial for group lists, feed, direct post access, rosters, and media. Verify admins can add and remove members, public Discover reveals only allowed information, and group leaders have explicitly assigned membership before deployment.

Validation version 36 built and installed in place. The build was explicitly
given empty pilot and local origins, which removes the obsolete built-in
Tailscale address. On the phone, the Community heading, Inbox, new-post button,
My groups and Latest posts were visible. The event area displays a real pinned
upcoming event when one is returned. The API was not connected during that
check. A targeted test run did not complete and was interrupted. Full
Community privacy and Bible Search acceptance remain pending.
