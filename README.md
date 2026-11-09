# WorkTrail — personal Android work-hours tracker (v0.3)

An offline-first Android app that automatically opens and closes work visits when the phone enters/exits a registered workplace. It compares credited time to a **custom Monday–Sunday schedule** and exports Excel-compatible CSV files. Intended for a personal timesheet, not a payroll-grade location record.

## What's new in v0.3

- **GitHub-style monthly work heatmap** on both **Today** and **History**, with month navigation and a compact monthly balance. Every tile shows the date and **net credited hours**.
- **Blue** means less than the day's configured target, **green** means within ±15 minutes, and **red** means more than the target. Stronger deviations produce stronger blue/red. Future days, days without targets or activity on rest days, and dates before targets are set are shown neutrally. The color compares against *that weekday's target*, not a fixed eight-hour day.
- Tap any date to see the day's recorded workplace **check-in and check-out times**, on-site and manually entered work, balance, and comments. Tap **View / edit day** to open detailed records and correction actions.
- **History** now shows the month grid, one chosen day's records, and compact edit/export actions rather than a never-ending list of visits and weekly rows.
- **System light/dark mode support**, including matching native date pickers, alert dialogs, editable text fields, buttons, cards and navigation; contrasting text stays readable on both schemes.
- **History → + Log hours** preselects the currently selected date for new work-from-home or other manual work entries.

## Other features

- Register multiple workplaces by name, GPS coordinates and a 100–1000 m geofence radius. A button can fill in your current location when you are physically at work.
- Optional workplace Wi-Fi SSID as a confirmation **hint** when a location event is received. Wi-Fi is not an independent trigger in this version; a mismatch does **not** suppress a location event.
- Record entrance/exit transitions while the app is closed, with Google Play services geofencing; re-register locations after device boot or app update.
- Store visit records on-device with SQLite, including geofence source, Wi-Fi confirmation result and manual correction notes.
- Merge re-entries within 10 minutes into one visit; prevent overlapping visits from double-counting in daily totals.
- Correct an arrival/departure or add a visit manually. Manually start or finish a visit when Android misses an event.
- Configure separate target hours for each weekday and an optional daily unpaid break with minimum-work threshold. Nothing is assumed about your employment contract until you save the schedule.
- See today's check-in and check-out timestamps for every visit, visit duration on that day, worked-time totals and balance.
- In **History**, select any calendar day to review its exact visit timestamps and edit them as needed.
- Enter **work from home** or other manual work by date, net duration, and optional comment. These hours have no invented arrival/departure times and are not charged an additional break; they count toward daily and weekly totals.
- See today's worked time and balance, an at-a-glance monthly calendar, and the current week's credited hours and balance.
- Export visits, manual-hour entries, or 365 days of daily totals as UTF-8 CSV for Excel, Google Sheets or LibreOffice. The output is *manual export*, not live synchronization.

## Build an APK (GitHub Actions)

1. Create a new GitHub repository and upload the **contents** of this project folder to its root, retaining `.github/workflows/build-apk.yml`.
2. Go to **Actions → Build Android APK → Run workflow** (or push to the `main` branch).
3. After the build finishes, open the workflow run and download the **WorkTrail-debug-apk** artifact ZIP.
4. Extract `app-debug.apk`, transfer it to your Samsung phone and open it to install. Android may ask you to allow installation from that source.
5. Open WorkTrail once, set your daily targets under **Schedule**, then add exact workplace building locations under **Places**.
6. Tap **Enable location tracking** and grant precise location and **Allow all the time**. You can separately enable notifications so arrivals and departures appear in your notifications.

For upgrading from the already-installed app, read [UPDATING.md](UPDATING.md). Your first APK may have been created using a temporary debug signing key, so Android may reject installing the new APK over it. Do not uninstall if it holds unexported records.

You can alternatively import this directory in Android Studio and run the `app` configuration. Build system: Java 17, Gradle 8.11.1, Android Gradle Plugin 8.9.2, compile SDK 35, target SDK 34 for the current private APK prototype, Google Play services Location 21.4.0. Before any Play Store publication, update the app to the store's then-current target API requirements and adapt its edge-to-edge UI.

## Important limitations

- **Android geofencing is not an exact clock-in sensor.** Events may be several minutes late, missed, or triggered near the boundary. The app logs the *time the broadcast is received* and provides manual correction. Compare it to actual arrival/departure during testing.
- Work locations should be precise building coordinates, not an entire city (e.g., *Herlev*); a 150 m radius is a practical starting point. DTU campus may require a specific building.
- If you register a geofence while already inside it, Android may emit an immediate entrance event; the recorded arrival will be the registration/detection time, **not** when you originally arrived.
- Workplaces may overlap geographically; WorkTrail closes an open session when another workplace starts. Commute between sites is **not** counted unless you add or adjust a record. Time spanning midnight is allocated to each calendar day by overlap.
- The configured break is deducted **once per day** when accumulated work reaches the threshold, even if you visited several locations. If your actual break varies, adjust your schedule or records; this version does not track breaks separately.
- Duration-only manual hours are assumed to be **net work** (breaks already excluded) and add directly to the daily total. Avoid double-counting time already covered by an on-site visit.
- Wi-Fi SSID may be unavailable on certain Android versions/configurations. It serves as a non-blocking verification note. Android's location service can work better with Wi-Fi scanning enabled even without connecting to a network.
- Keep Google Play services, location services and necessary battery/background permissions enabled. This app does not record continuous GPS trails.
- No cloud account, live Google Sheets sync, auto-generated map images, holiday calendar, or signed release build yet. These are possible follow-up features.
- For reliable in-place APK upgrades, use a **persistent signing key**. The build workflow can read one from GitHub Actions secrets; see [UPDATING.md](UPDATING.md).

## Data and privacy

All visit information is saved in an on-device SQLite database. The app does not send times or workplace data to a custom server. Android's Google Play services handles geofence detection. Back up exported CSV files before uninstalling or clearing app data; this version intentionally disables Android auto backup.

## Test before trusting it

For the first 3–5 working days, compare the automatic event times to your real arrivals and departures. Try a normal arrival, a short walk outside, a full departure, a phone reboot and a day with both sites. Review the **History** tab and correct missed events. If you need minute-level precision for timesheets, a deliberate tap, NFC tag, or workplace Wi-Fi confirmation workflow may be more dependable than background geofencing alone.

## Development roadmap

1. **v0.1:** Native location tracking, local timesheet, custom working schedule and CSV export.
2. **v0.2:** Exact timestamp history, editable visits, duration-only work-from-home entries and comments.
3. **v0.3 (current):** Interactive monthly hour-balance heatmap, compact day-focused history and native system light/dark themes.
4. **Future:** Stronger Wi-Fi-assisted state changes, editable break intervals, optional Google Sheets sync and database backup/restore.

## File layout

- `app/src/main/java/dk/worktrail/app/MainActivity.java` — native screens, forms and CSV export.
- `TrackerDb.java` — local SQLite workplaces, visits, manual-duration entries, and event log (schema v2 preserves existing visits).
- `Geofences.java`, `GeofenceReceiver.java`, `BootReceiver.java` — geofence registration, transitions, reboot recovery.
- `Schedule.java`, `TimeMath.java`, `HeatmapColors.java` — weekday targets, duration and break calculations, and color interpolation.
- `app/src/main/res/values*/styles.xml` — matching light and dark system themes, including dialogs.
- `app/src/test/` — calculation tests.
- `.github/workflows/build-apk.yml` — APK build and test workflow.
- `tools/SmokeTest.java` — dependency-free calculation smoke tests, runnable with a JDK compiler.
