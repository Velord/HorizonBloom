# Maven Central release preparation

Horizon Bloom's Maven coordinates are `io.github.velord:horizon-bloom:<version>`
for the renderer and `io.github.velord.horizonbloom:demo:<version>` for the
optional editor. Only `:horizon-bloom` and `:demo` are configured for
publication. Version 0.1.0 contains only the renderer; the demo artifact starts
with a later release. The demo applications are not part of Maven Central.

## One-time publisher setup

1. Create an account at https://central.sonatype.com/.
2. Verify the registered `io.github.velord` namespace by demonstrating control of
   the GitHub account `Velord` using the Sonatype verification repository
   (https://github.com/Velord/foidjyap2x). The repository must be public, and
   the namespace must show Verified in Central Portal before publishing.
3. Create a GPG signing key and distribute its public key to a public key server.
   Keep the private key and its passphrase outside this repository.
4. In Central Portal, generate a **publishing user token**. The token username
   and password are distinct from your Central Portal sign-in credentials.
5. In GitHub, create environment `maven-central`. Configure required
   reviewers if you want an explicit environment approval before an upload.
6. Add these repository or environment *secrets* (not Gradle properties):
   - `MAVEN_CENTRAL_USERNAME`: publishing user-token username
   - `MAVEN_CENTRAL_PASSWORD`: publishing user-token password
   - `MAVEN_SIGNING_KEY`: complete ASCII-armored private PGP key
   - `MAVEN_SIGNING_PASSWORD`: corresponding key passphrase

7. Add GitHub environment or repository **variable** `MAVEN_POM_DEVELOPER_EMAIL`
   containing a contact address you explicitly agree to publish publicly in
   the Maven POM. The workflow refuses to upload unless it is provided.
   A personal Git commit email is **not** used automatically.

Do not copy private keys or credentials into this repository or into logs.

## Verify before upload

From a checked-out release commit with JDK 21:

```shell
./gradlew :horizon-bloom:compileAndroidMain :horizon-bloom:compileKotlinDesktop
./gradlew :demo:compileAndroidMain :demo:compileKotlinDesktop
./gradlew :demo-desktop:compileKotlinDesktop :demo-android:assembleDebug
./gradlew :horizon-bloom:generatePomFileForKotlinMultiplatformPublication
./gradlew :horizon-bloom:checkPomFileForKotlinMultiplatformPublication
./gradlew :demo:generatePomFileForKotlinMultiplatformPublication
./gradlew :demo:checkPomFileForKotlinMultiplatformPublication
```

Review both generated Kotlin Multiplatform POMs, including the Apache 2.0 license,
developer, SCM metadata, and both Maven groups. Check that the demo publication
depends on the matching renderer version. Supply the chosen public contact
through `POM_DEVELOPER_EMAIL` in a private Gradle property or
`ORG_GRADLE_PROJECT_POM_DEVELOPER_EMAIL` environment variable when checking
POM metadata. The email is embedded in the published POM.

On macOS, compile both iOS targets for `:demo` and `:demo-ios`, build the Xcode
app, and inspect all platform publication artifacts. The publish workflow uses
macOS so all platform variants can be assembled.

## Next release (only when explicitly approved)

1. Review the release commit and verify the GitHub repository contains a
   complete `LICENSE` file, a valid public-facing README, and no secrets.
2. Ensure `io.github.velord` is verified, all four GitHub secrets and the public
   contact email variable are installed, and the `maven-central` environment
   is configured.
3. After authorizing release, push the release commit and create/push a new
   `vX.Y.Z` tag (for example, `v0.1.1`). **Do not reuse version 0.1.0.**
4. Manually start the **Prepare Maven Central Deployment** workflow using the
   tag's version without the `v` prefix. No push, tag, or GitHub Release event
   starts this workflow automatically.
5. The workflow uploads signed artifacts to Central Portal. It does **not**
   publish them for Maven Central consumption.
6. Review every deployment in https://central.sonatype.com/publishing/deployments
   and manually select **Publish** only after approval.
7. After Central indexing completes, verify both artifacts from a separate KMP
   project using the same release version. Confirm the demo resolves the
   renderer transitively.

Review license notices and release documentation before each public release.

References:
- https://vanniktech.github.io/gradle-maven-publish-plugin/central/
- https://central.sonatype.org/register/namespace/
- https://central.sonatype.org/publish/generate-portal-token/
- https://kotlinlang.org/docs/multiplatform/multiplatform-publish-libraries-to-maven.html
