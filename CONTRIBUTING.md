# Contributing to FlowForge

Thank you for your interest in contributing to FlowForge! This document provides guidelines and instructions for contributing.

## Quick Links

- [Code of Conduct](./CODE_OF_CONDUCT.md)
- [Issue Tracker](https://github.com/flowforge/flowforge/issues)
- [Discussions](https://github.com/flowforge/flowforge/discussions)

## How Can I Contribute?

### Reporting Bugs

Before submitting a bug report:
1. Check the [existing issues](https://github.com/flowforge/flowforge/issues)
2. Upgrade to the latest version - the bug might already be fixed
3. Gather relevant information (Java version, Spring Boot version, workflow configuration)

Use the [bug report template](./.github/ISSUE_TEMPLATE/bug_report.md) when creating issues.

### Suggesting Features

We welcome feature suggestions! Please:
1. Check existing issues and discussions first
2. Describe the use case and why it would benefit the community
3. Provide code examples if possible

Use the [feature request template](./.github/ISSUE_TEMPLATE/feature_request.md).

### Pull Requests

#### Development Setup

1. **Prerequisites**
   - JDK 21 or later
   - Gradle (wrapper included)

2. **Fork and Clone**
   ```bash
   git clone https://github.com/YOUR_USERNAME/flowforge.git
   cd flowforge
   ```

3. **Build and Test**
   ```bash
   ./gradlew build
   ```

4. **Run Specific Tests**
   ```bash
   ./gradlew :flowforge-core:test
   ./gradlew :flowforge-spring-boot-autoconfigure:test
   ```

#### Code Style

We follow standard Java conventions with these specifics:

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Naming**: 
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `SCREAMING_SNAKE_CASE`
  - Packages: `lowercase`

#### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(core): add parallel task execution support
fix(orchestrator): resolve deadlock in concurrent workflows
docs(readme): update installation instructions
test(registry): add edge case tests for duplicate workflow IDs
refactor: extract method for cleaner separation
```

#### Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/my-new-feature
   # or
   git checkout -b fix/issue-description
   ```

2. **Make your changes**
   - Write code following our style guidelines
   - Add/update tests for new functionality
   - Update documentation if needed

3. **Ensure all tests pass**
   ```bash
   ./gradlew build
   ```

4. **Submit a Pull Request**
   - Fill out the PR template completely
   - Reference related issues with `Fixes #123` or `Relates to #456`
   - Ensure CI passes

5. **Code Review**
   - Be responsive to feedback
   - Make requested changes in new commits (don't force push during review)
   - Once approved, maintainers will squash and merge

## Project Structure

```
flowforge/
├── flowforge-core/              # Core engine - no Spring dependencies
├── flowforge-spring-boot-starter/       # Spring Boot starter
├── flowforge-spring-boot-autoconfigure/ # Auto-configuration
├── flowforge-bom/              # Bill of Materials
└── flowforge-stress-harness/   # Performance testing tools
```

### Module Responsibilities

| Module | Description |
|--------|-------------|
| `flowforge-core` | Reactive workflow orchestration engine |
| `flowforge-spring-boot-autoconfigure` | Spring Boot integration |
| `flowforge-spring-boot-starter` | Convenience dependency |

## Testing Guidelines

### Unit Tests
- Each class should have a corresponding `*Test.java`
- Aim for meaningful assertions, not just coverage
- Test edge cases and error conditions

### Integration Tests
- Spring Boot integration tests in `*IntegrationTest.java`
- Use `@SpringBootTest` with `webEnvironment = NONE`

### Test Naming
```java
@Test
void should_execute_linear_workflow() { }

@Test
void should_throw_exception_on_type_mismatch() { }

@Test
void handle_concurrent_access_gracefully() { }
```

## Questions?

Feel free to:
- Open a [discussion](https://github.com/flowforge/flowforge/discussions)
- Ask in our community channels
- Email the maintainers

## License

By contributing to FlowForge, you agree that your contributions will be licensed under the Apache License 2.0.
