name: Open Source Library Assistance
description: Request help adapting FlowForge for your project
title: '[HELP] '
labels: ['question']
assignees: []

body:
  - type: markdown
    attributes:
      value: |
        ## Getting Help

        For questions about using FlowForge, please use [GitHub Discussions](https://github.com/flowforge/flowforge/discussions) instead of issues.

        This template is for questions about adapting the library to specific needs.

  - type: textarea
    id: use-case
    attributes:
      label: Use Case Description
      description: Describe your workflow orchestration needs
      placeholder: What business process are you trying to automate?

  - type: textarea
    id: current-approach
    attributes:
      label: Current Approach
      description: How are you currently solving this problem?
      placeholder: |
        - Current workflow implementation
        - Technologies being used
        - Pain points with current solution

  - type: textarea
    id: flowforge-approach
    attributes:
      label: FlowForge Adaptation Questions
      description: What specific aspects of FlowForge do you need help with?
      placeholder: |
        1. Task handler design
        2. Integration with existing services
        3. Error handling strategies
        4. Testing approaches
        5. Other questions

  - type: textarea
    id: code
    attributes:
      label: Code Examples
      description: Relevant code snippets or workflow definitions
      placeholder: |
        ```java
        // Your workflow or task code
        ```
