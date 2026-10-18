# Third-party components

LuckyLotto's own source is GPL-3.0-or-later. Dependencies keep their upstream licenses.

| Component | Use | Upstream license |
| --- | --- | --- |
| AndroidX Compose, Material 3, Activity, Lifecycle, Core | Android UI and lifecycle | Apache-2.0 |
| Kotlin standard library and kotlinx.coroutines | Language/runtime and concurrency | Apache-2.0 |
| Material icons | Interface icons | Apache-2.0 |
| Gradle wrapper | Build bootstrap, not app runtime | Apache-2.0 |
| JUnit 4 | Local tests only | EPL-1.0 |
| JSON-java | Local tests only; Android provides its own org.json at runtime | Public domain / upstream license |

Pinned versions and transitive dependencies are resolved in the Gradle build. Obtain corresponding upstream sources through Google Maven, Maven Central and the projects' official source repositories. The Apache license is reproduced in `LICENSES/Apache-2.0.txt`.

Game names, rules and public draw facts are attributed to OLG.ca in `docs/SOURCES.md`. No OLG logos, website illustrations or proprietary application code are distributed.
