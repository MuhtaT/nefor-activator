# Nefor Activator

Multi-module Gradle project implementing a licencing enforcement stack for Spigot/Paper servers.

## Modules

- ctivator-api – lightweight SDK for protected plugins. Provides service interfaces, annotations, and guard helpers.
- ctivator-plugin – server-side plugin that validates licenses, handles offline cache, issues events, and disables dependent plugins.

## Building

`
./gradlew build
`

Artifacts:

- ctivator-api/build/libs/activator-api-<version>.jar
- ctivator-plugin/build/libs/activator-plugin-<version>-all.jar (shaded with runtime dependencies)

## Configuration

1. Drop ctivator-plugin-*.jar into the server plugins/ directory and start once to generate config.yml.
2. Populate licenseKey, secret, and endpoint parameters.
3. Optional: adjust offline, enforcement, and lang files under plugins/Activator/lang/.
4. Secure plugins declare depend: [Activator] in plugin.yml and call ActivationGuard.guardOrDisable() in onLoad.

## Commands

- /activator status – show current license status, fingerprint, products.
- /activator refresh – force refresh from licensing server.
- /activator diag – execute connectivity diagnostics.
- /activator reload – reload configuration and restart validation pipeline.

## SDK Usage

Add ctivator-api as a compileOnly dependency and optionally shade it into protected plugins. Use @RequiresLicense and ActivationGuard to enforce checks.
