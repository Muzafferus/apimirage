# Publishing ApiMirage To Maven Central

This repository is configured to publish the Android library modules to Maven Central through Sonatype Central Portal's OSSRH staging compatibility endpoint.

Published modules:

- `io.github.muzafferus:apimirage-core`
- `io.github.muzafferus:apimirage-retrofit`

The GitHub Actions workflow publishes on tags shaped like `v0.1.0`.

## What Is Already Configured In This Repo

- `maven-publish` and `signing` for `:apimirage-core`
- `maven-publish` and `signing` for `:apimirage-retrofit`
- `sources.jar` generation for Android `release`
- placeholder `javadoc.jar` generation using the repository README
- required POM metadata for Maven Central
- GitHub Actions workflow at `.github/workflows/publish-maven-central.yml`
- automatic follow-up call to Sonatype's manual upload endpoint so Maven-like Gradle publishing becomes visible in Central Portal

## Required GitHub Secrets

Add these repository secrets before pushing a release tag:

- `CENTRAL_USERNAME`
- `CENTRAL_PASSWORD`
- `SIGNING_KEY`
- `SIGNING_PASSWORD`

`SIGNING_KEY` should contain your ASCII-armored private key.

## Release Flow

1. Make sure the Sonatype namespace is verified.
2. Make sure the Portal token is active.
3. Make sure the GitHub secrets are present.
4. Push a tag such as `v0.1.0`.
5. GitHub Actions will:
   - run unit tests
   - publish both release publications to the Sonatype staging compatibility repository
   - call Sonatype's `manual/upload/defaultRepository/<namespace>?publishing_type=automatic` endpoint
6. If validation succeeds, Sonatype will release the deployment to Maven Central.

## Local Dry Run Commands

Build and verify:

```bash
./gradlew :apimirage-core:testDebugUnitTest :apimirage-retrofit:testDebugUnitTest :sample-app:testDebugUnitTest
```

Publish to local Maven cache:

```bash
./gradlew :apimirage-core:publishReleasePublicationToMavenLocal :apimirage-retrofit:publishReleasePublicationToMavenLocal
```

## Notes

- `VERSION_NAME` defaults to `0.1.0-SNAPSHOT` locally.
- The GitHub Actions workflow overrides `VERSION_NAME` from the pushed tag value.
- The current setup assumes the repository is published under the `io.github.muzafferus` namespace.
- The repository uses Apache 2.0 metadata and license text.
