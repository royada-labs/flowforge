/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  // Sidebar para la documentación base - FLATTENED as per user request
  docsSidebar: [
    'getting-started/index',
    'project-structure',
    'core-concepts/index',
    'api-reference/index',
    'observability/index',
    'examples/index',
    'troubleshooting',
  ],

  // Sidebar exclusivo para el tutorial incremental
  tutorialSidebar: [
    {
      type: 'doc',
      id: 'tutorial/index',
      label: 'Introduction',
    },
    {
      type: 'category',
      label: 'Beginners Tutorial',
      items: [
        'tutorial/level1-basics',
        'tutorial/level2-sequential',
        'tutorial/level3-parallel',
        'tutorial/level4-resilience',
        'tutorial/level5-advanced',
      ],
    },
  ],
};

module.exports = sidebars;
