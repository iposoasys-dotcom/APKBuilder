# APKBuilder v2

Phone-friendly Android project editor/generator.

## Included
- Java/Kotlin project generation
- Project explorer
- Text/code editor
- Save files
- Build console
- Project ZIP export
- GitHub Actions build

## Important limitation
This version builds the APKBuilder application itself through GitHub Actions.
The in-app arbitrary Android-project compiler is intentionally not represented as
a fake compiler. A true standalone compiler needs compatible Android SDK/build
tools and Java/Kotlin compiler components.

## Build
Upload this repository to GitHub, then:
Actions -> Build APKBuilder -> Run workflow.
