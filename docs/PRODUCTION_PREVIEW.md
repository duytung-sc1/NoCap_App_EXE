# Internal production preview

Build from the Android repository with:

```powershell
.\gradlew.bat '-Dorg.gradle.java.home=C:/Program Files/Java/jdk-23' test lintDebug assembleProductionPreview --console=plain
```

Output: `app/build/outputs/apk/productionPreview/app-productionPreview.apk`.

| Variant | API | Signing | Debuggable |
| --- | --- | --- | --- |
| debug | https://nocap-ebook-api-qa.buiminhhien001.workers.dev | Android debug key | Yes |
| productionPreview | https://nocap-ebook-api.buiminhhien001.workers.dev | Android debug key | No |
| release | https://nocap-ebook-api.buiminhhien001.workers.dev | Existing release configuration; no key added | No |

The preview inherits release settings and the current launcher/adaptive/monochrome icons. Its Play product ID is always empty, so real purchases stay unavailable even if a local Gradle property supplies a product ID. No release keystore or signing secrets are needed. This APK is for internal testing only.

## Installation and data

The package remains `com.nocap.app`. It replaces an installed build with the same package and signing certificate; it does not install alongside it. Use the same workstation debug key for compatible updates:

```powershell
adb -s <serial> install -r app/build/outputs/apk/productionPreview/app-productionPreview.apk
```

`-r` preserves app data. A signature mismatch must be resolved without uninstalling or clearing existing data. A later APK signed with a release key cannot directly update this debug-signed installation.

Installing over a QA build also preserves cached QA sessions and local profile data. Before changing environments, preserve pending work in the original environment and sign out using the app; then sign in with a production account after installation. QA credentials are not production credentials. The preview connects to real production data and is not a disposable QA environment. No test accounts, tokens, or fixtures are bundled.
