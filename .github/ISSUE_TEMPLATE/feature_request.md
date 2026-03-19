name: Feature Request
description: Suggest a new feature or enhancement
title: '[FEATURE] '
labels: ['enhancement']
assignees: []

body:
  - type: markdown
    attributes:
      value: |
        ## Feature Description
        A clear and concise description of the feature you'd like to request.

  - type: textarea
    id: problem
    attributes:
      label: Problem Statement
      description: What problem does this feature solve?
      placeholder: Describe the problem or pain point you're experiencing

  - type: textarea
    id: solution
    attributes:
      label: Proposed Solution
      description: How should this feature work?
      placeholder: Describe your proposed solution

  - type: textarea
    id: alternatives
    attributes:
      label: Alternatives Considered
      description: What other approaches have you considered?
      placeholder: List any alternative solutions or workarounds

  - type: textarea
    id: example
    attributes:
      label: Usage Example
      description: Show how this feature would be used
      placeholder: |
        ```java
        // Example usage of the proposed feature
        ```

  - type: textarea
    id: context
    attributes:
      label: Additional Context
      placeholder: Any other context, screenshots, or references

  - type: checkboxes
    id: terms
    attributes:
      label: Checklist
      options:
        - label: I have searched existing issues and discussions
          required: true
        - label: This is a new feature, not a duplicate request
          required: true
        - label: I'm willing to help implement this feature
          required: false
