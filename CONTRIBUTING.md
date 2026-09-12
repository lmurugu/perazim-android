# Contributing to Perazim Mission Church Android App

Thank you for your interest in contributing to the official Android application of **Perazim Mission Church**! We welcome bug fixes, performance improvements, feature enhancements, localization/translations, and UI polish from the developer and faith community.

---

## Code of Conduct

All contributors are expected to uphold our [Code of Conduct](CODE_OF_CONDUCT.md) to ensure a respectful, welcoming, and Christ-centered environment for everyone.

---

## How to Contribute

### 1. Reporting Issues
- Check the [issue tracker](https://github.com/lmurugu/perazim-android/issues) to ensure your issue hasn't already been reported.
- Use our **Bug Report** template to provide detailed steps to reproduce, device model, Android OS version, and logcat output.

### 2. Proposing Enhancements
- Open a **Feature Request** issue to describe the motivation, proposed UI/behavior, and technical feasibility.
- Discuss with repository maintainers before writing extensive code.

### 3. Submitting Pull Requests
1. **Fork the repository** on GitHub.
2. **Create a topic branch** from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```
3. **Commit your changes**:
   - Write clear, concise commit messages following the [Conventional Commits](https://www.conventionalcommits.org/) specification:
     - `feat:` for new capabilities
     - `fix:` for bug fixes
     - `docs:` for documentation updates
     - `refactor:` for code restructuring without behavioral changes
     - `perf:` for performance optimizations
4. **Test your build locally**:
   - Verify that `./gradlew assembleDebug` compiles without warnings or errors.
   - Test on an Android 8.0+ physical device or emulator.
   - Verify that memory usage and frame rates remain optimal.
5. **Push to your fork** and submit a Pull Request to `main`.
6. Fill out the **Pull Request Template** with a description of changes and screenshots for UI modifications.

---

## Code Standards & Style Guidelines

- **Native Android Standards**: Prefer native Android SDK components. Avoid unnecessary heavy external dependencies to keep the APK lightweight and battery-efficient.
- **Java Language Level**: Keep code compatible with Java 8/17/21 LTS compilers.
- **Formatting**:
  - Indent with 4 spaces (no tabs).
  - Use camelCase for methods and variables, PascalCase for classes and interfaces, and UPPER_SNAKE_CASE for constants.
  - Follow Android Open Source Project (AOSP) Java style conventions.
- **Offline Reliability**: Ensure all core assets, hymnals, and scripture readings remain accessible even when the device is disconnected from the internet.

---

## Community & Questions

For questions or guidance regarding development, reach out to the development team at `info@perazimchurch.org` or open a GitHub discussion.
