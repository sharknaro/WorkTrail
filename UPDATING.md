# How to update WorkTrail from GitHub Desktop (Windows)

## Updating the source and building an APK

1. Download the updated `WorkTrail-Android-Source.zip` from this conversation.
2. Extract it in Windows. Open the inner **WorkTrail** folder until you see `app`, `.github`, and `settings.gradle`.
3. Open **GitHub Desktop** and select your existing WorkTrail repository.
4. Choose **Repository → Show in Explorer** to open its local repository folder.
5. Copy the **contents** of the updated WorkTrail folder into the repository folder. Accept replacing files when Windows asks. Do **not** replace or remove the repository's hidden `.git` folder.
6. In GitHub Desktop, enter commit summary `WorkTrail v0.3 - monthly heatmap and adaptive themes`, click **Commit to main**, then **Push origin**.
7. In the browser, open the repository's **Actions → Build Android APK**, wait for a successful run, then download the **WorkTrail-debug-apk** artifact.
8. Extract the artifact ZIP to obtain `app-debug.apk`. Transfer it to your phone.

## Important: installing an update over the existing app

Android requires the same application ID, a compatible or higher version code, and **the same app-signing certificate** to install an update without uninstalling. This project maintains the same application ID and automatically increments its GitHub build version code. However, the **v0.1 GitHub debug APK was signed with the CI runner's temporary debug key**. The signing key from that first build is probably unavailable for future builds.

Therefore (this applies to **v0.3** as well):

- If Android installs the new APK over your existing version: excellent, verify that the History is intact.
- If Android says *App not installed* or *conflicting package*: do **not** keep retrying. The likely reason is a different signing key. The initial transition may require uninstalling v0.1 first. **Uninstalling deletes locally stored workplace and timesheet data.**
- Before uninstalling, open the existing app and export visit, manual-hour and daily CSVs from **History**, if you have any data. These are archival copies; v0.3 does not yet import them back into the app.
- Once the old app is uninstalled, install v0.3 afresh. Then set up persistent signing **before** building the next version if you want future upgrades to retain your on-device records.

## Recommended: use one permanent signing key for future versions

The workflow supports a **user-owned** permanent key through GitHub Actions secrets; do not add the `.jks` key directly to your source repository or share it publicly.

### 1. Generate your signing key locally

If the Java Development Kit is installed on your Windows PC, open a terminal in a private folder and run:

```powershell
keytool -genkeypair -v -keystore worktrail-signing.jks -alias worktrail -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12
```

Choose a strong password and keep the file and password securely backed up. For PKCS12, the key password typically matches the keystore password. If `keytool` is not found, install JDK 17 first (for example, via the official Eclipse Temurin installer) or generate a keystore in Android Studio.

### 2. Store the key in GitHub repository secrets

In PowerShell, with the terminal in the same folder as the `.jks` file, run:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path './worktrail-signing.jks'))) | Set-Clipboard
```

In GitHub, go to **Repository → Settings → Secrets and variables → Actions → New repository secret** and create:

| Secret name | Value |
| --- | --- |
| `WORKTRAIL_KEYSTORE_BASE64` | The Base64 string copied to your clipboard |
| `WORKTRAIL_STORE_PASSWORD` | Your keystore password |
| `WORKTRAIL_SIGNING_ALIAS` | `worktrail` |
| `WORKTRAIL_KEY_PASSWORD` | Your key password (normally the same as the store password for PKCS12) |

The workflow decodes the key into temporary runner storage and signs the APK with it. Never commit the `.jks` or passwords to Git; keep your own secure offline backup. **The first install signed by this permanent key may still require removing the original temporary-key v0.1 app.** Once installed, future APKs signed with this same key can update in place, preserving your local database.

### 3. Verify on each update

After installing an update, open WorkTrail and check that the old **History**, registered **Places**, and **Schedule** are still present. Only delete an older APK download after verifying the installed app.

## Test the v0.3 design before commuting

1. In **Schedule**, set a target for today's weekday (for example 7:24). The heatmap colors need daily targets to be meaningful.
2. Switch the Samsung **Settings → Display → Light / Dark** setting. Reopen WorkTrail and test **Schedule → Edit**, **History → + Log hours**, and a calendar tile. Text should be readable in each dialog, not white on white.
3. In **History**, tap a square for today and tap **View / edit day**. Try the previous/next month buttons. The selected date should show check-ins, check-outs and manual notes, if any.
4. Use **History → + Log hours** to add a **work from home** entry with a comment to a past date. The selected date's hours and color should change. Try a day with fewer than target hours (blue), within 15 minutes (green), and more than target (red). Test on a day with no on-site visit to avoid double-counting.
5. Correct or delete test entries by opening their date under **History**. Check CSV exports under **Export timesheets**.
6. Finally, test real geofence arrivals/departures over several working days; background location events are approximate, so compare their timestamps with when you actually entered/left.
