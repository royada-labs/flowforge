name: Bug Report
description: Create a report to help us improve
title: '[BUG] '
labels: ['bug']
assignees: []

body:
  - type: markdown
    attributes:
      value: |
        ## Bug Description
        A clear and concise description of what the bug is.

  - type: textarea
    id: description
    attributes:
      label: Describe the bug
      placeholder: Tell us what happened and what you expected to happen
    validations:
      required: true

  - type: textarea
    id: steps
    attributes:
      label: Steps to Reproduce
      placeholder: |
        1. Go to '...'
        2. Create workflow with '...'
        3. Execute with '...'
        4. See error
    validations:
      required: true

  - type: textarea
    id: logs
    attributes:
      label: Relevant log output
      placeholder: |
        Paste relevant log output here
        ```

        ```

  - type: dropdown
    id: java-version
    attributes:
      label: Java version
      options:
        - 21
        - 22
        - Other
    validations:
      required: true

  - type: dropdown
    id: spring-boot
    attributes:
      label: Spring Boot version
      options:
        - 3.4.x
        - 3.3.x
        - 3.2.x
        - Other
        - N/A (core only)

  - type: textarea
    id: context
    attributes:
      label: Workflow Context
      description: Workflow configuration or DSL code that reproduces the bug
      placeholder: |
        ```java
        // Your workflow configuration here
        ```

  - type: checkboxes
    id: terms
    attributes:
      label: Checklist
      options:
        - label: I have searched the existing issues
          required: true
        - label: I have verified the bug exists in the latest version
          required: true
        - label: I have provided a complete reproduction case
          required: true
