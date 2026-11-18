# ADR 0001: No cookie banner in the app

Status: Accepted (pending owner confirmation, plan gate C.1)
Date: 2026-11-18

## Context
The Android app uses no cookies or web tracking. A cookie banner would add friction without any consent choice to make.

## Decision
The app shows no cookie banner. The Cookie Policy states that the app sets no non-essential cookies or trackers.

## Consequences
If any website or in-app web view starts using cookies, analytics or trackers, the change must add a consent banner (Accept all, Reject non-essential, Manage) before that change ships, and update the Cookie Policy and third-party inventory in the same change.
