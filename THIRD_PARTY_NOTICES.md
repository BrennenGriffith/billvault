# Third-party notices

Android uses AndroidX/Compose, Lifecycle, Kotlin/coroutines, and OkHttp under Apache License 2.0.
Notices and license text are bundled under `android/app/src/main/assets/licenses`.
Desktop TLS certificate creation uses Bouncy Castle 1.86 under its MIT-style license;
dependency JARs retain their license files. Versions are pinned in pom.xml and android/app/build.gradle.kts.

BillVault's own source is MIT-licensed. Third-party packages retain their original licenses.
See frontend/package-lock.json, integrations/mcp/package-lock.json, and pom.xml for exact dependency versions.

The self-hosted DM Sans and Manrope fonts are distributed under SIL Open Font License 1.1.
Their full notices, along with direct frontend runtime dependency notices, are included in
[frontend/public/licenses](frontend/public/licenses) and copied into the static build and personal JAR.
No fonts are modified. React/React DOM and lucide-react retain their package copyright and license notices.

## Desktop packages

JavaFX (OpenJFX) 25.0.1 is distributed under GPL version 2 with the Classpath Exception.
See [OpenJFX license](https://github.com/openjdk/jfx25u/blob/25.0.1-ga/LICENSE) and
[additional assembly notices](https://github.com/openjdk/jfx25u/blob/25.0.1-ga/ASSEMBLY_EXCEPTION).
Native packages bundle an Eclipse Temurin runtime, also GPLv2 with the Classpath Exception;
its license and component notices are retained under the packaged `runtime/legal` directory.
Dependency JARs retain their embedded META-INF license/notice files. OpenJFX's full license and
graphics-component notices are included under `desktop/licenses/OpenJFX` and copied into desktop packages.
Corresponding OpenJFX source is available at the linked `25.0.1-ga` source tag.
The desktop edition does not bundle the React frontend or its fonts.
Packaging tools (WiX/appimagetool) are build tools, not part of BillVault's application source license.

## Optional MCP adapter

The separately installed Node.js adapter uses the MIT-licensed Model Context Protocol SDK
and Zod. Their license files and dependency notices remain in their npm packages.
The adapter does not bundle a model-provider SDK or a model runtime.
