# WorkTrail implementation plan

## Product goal

Maintain a private, automatically generated record of the time a user spends at pre-registered workplace locations and calculate over-/under-work against a configurable weekly schedule.

## v0.1 — offline Android prototype (implemented in this source)

| Area | Decision | Verification |
| --- | --- | --- |
| Workplace registration | Name, latitude, longitude, circular radius; optional workplace Wi-Fi SSID | Create Herlev and DTU sites and inspect saved details |
| Automatic detection | Android Google Play services geofence ENTER and EXIT broadcasts | Arrive and leave each building with phone locked |
| Startup | Re-register fences after reboot/app update | Reboot phone and check next entrance |
| Storage | On-device SQLite; raw transitions plus visits | Confirm History persists after process restart |
| Time accounting | Union overlapping visit intervals; split across calendar-day boundaries | Test duplicate/overlapping and overnight entries |
| Weekly targets | 7 independent day goals, break and break threshold | Set custom schedule; check positive/negative balances |
| Corrections | Manual check-in/out; edit missing or delayed entries | Fix a late exit and check balances update |
| Export | CSV visit and daily files via Android file picker | Open file in Excel; verify local timestamps and numeric minutes |
| Privacy | No custom server, no continuous GPS path | App usable offline after setup; exported files remain user-owned |

## Accuracy model

The initial recorded event time is the time Android delivers the location broadcast. It is an estimate rather than a guaranteed timestamp at the building door; delays of minutes are possible. The app records both visits and raw events locally. Corrections are marked as manual. Wi-Fi SSIDs currently annotate geofence events but are not independently polled or used to end a visit.

## v0.2 — field-testing improvements

1. Test at each building for 3–5 workdays, comparing event timestamps with the user's own observations.
2. Tune per-place radius, validate false positives and missed exits, and adjust the 10-minute re-entry merge rule.
3. Add independent Wi-Fi connection/disconnection monitoring if the phone and workplace network make it reliable, with an explicit precedence policy.
4. Add an end-of-day review reminder, separate break intervals and a calendar for leave/public holidays.
5. Add an on-device warning when location permission, location services, battery restriction, or Google Play services breaks tracking.

## v0.3 — optional quality-of-life features

1. Opt-in Google Sheets sync with a secure, user-controlled endpoint and offline retry queue.
2. A generated location/date/time record image (not an automatic Google Maps screen capture).
3. Backup/restore, PDF weekly report, and a map-based workplace selector.
4. Revisit Android target API and UI insets before any Play Store release.

## Product choices to confirm

- Exact Herlev building and exact DTU building(s), or coordinates obtained on site using **Use my current GPS location**.
- Expected hours for Monday through Sunday (rather than assuming 37 or 40 hours).
- Unpaid break policy: for example, 30 minutes after 6 hours elapsed, or a different rule.
- Whether the trip between Herlev and DTU counts as work or is excluded as transit (current prototype excludes time between visits).
- Whether to keep the data only on the phone or also sync to Google Sheets.

## Definition of ready-to-use

The APK builds successfully on GitHub Actions, installs on the actual Samsung phone, saves two places and a schedule, records at least three consecutive working days including departures, survives reboot, and exports a CSV that reconciles with manual observation. Until field testing passes, use corrections and do not treat the automatic timestamps as payroll-grade proof.
