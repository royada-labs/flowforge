# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.1] - 2026-03-15

### Changed
- **Internal Optimizations**: Performed core engine optimizations to enhance reactive block normalization and execution flow.
- **Dependency Alignment**: Further refined BOM dependencies for better compatibility with Spring Boot 4.0.
- **Documentation**: Updated `README.md` and `AI_CONTEXT.md` to reflect latest architecture.

## [0.2.0] - 2026-03-15

### Added
- **FlowForge BOM (`flowforge-bom`)**: Introduced a Bill of Materials module to centralize dependency management for consumers.
- **`FlowForgeClient.executeResult`**: Added a simplified method to execute workflows and retrieve only the terminal result.
- **Runtime Identity**: Enhanced observability with unique execution identifiers and improved logging context.
- **Git Confidence**: Added a comprehensive `.gitignore` to the project root.

### Changed
- **Dependency Management**: Migrated to Gradle Version Catalogs (`libs.versions.toml`) for better dependency tracking.
- **Spring Boot Alignment**: Aligned all dependencies with Spring Boot 4.0.3 and implemented BOM-based versioning.
- **Package Refactoring**: Relocated core packages from `autoconfigure` to `core` for better separation of concerns.
- **Build System**: Updated Gradle configuration to support multi-module publishing and centralized versioning via `gradle.properties`.

### Fixed
- Improved resilience in `FlowForgeClient` when handling empty results.
- Fixed various race conditions in the `StressHarness` testing module.

## [0.1.0] - 2026-03-13

### Added
- **Core Engine**: Initial release of the reactive workflow orchestration engine.
- **DAG Orchestration**: Implementation of Directed Acyclic Graph validation and execution.
- **Spring Boot Integration**: Automated discovery of tasks (`@FlowTask`) and workflows (`@FlowWorkflow`).
- **Reactive DSL**: Fluent API for defining workflows with `then`, `fork`, and `join` operations.
- **Execution Policies**: Basic support for `RetryPolicy` and `TimeoutPolicy`.
- **Stress Harness**: Load testing utility for validating engine performance under high concurrency.
- **Documentation**: Initial `README.md` and `AI_CONTEXT.md` documentation.

[Unreleased]: https://github.com/tugrandsolutions/flowforge/compare/v0.3.1...HEAD
[0.3.1]: https://github.com/tugrandsolutions/flowforge/compare/v0.2.0...v0.3.1
[0.2.0]: https://github.com/tugrandsolutions/flowforge/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/tugrandsolutions/flowforge/releases/tag/v0.1.0
