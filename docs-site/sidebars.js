/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  // Sidebar para la documentación base
  docsSidebar: [
    {
      type: 'category',
      label: 'Getting Started',
      items: ['getting-started/index'],
    },
    {
      type: 'category',
      label: 'Core Documentation',
      items: [
        'core-concepts/index',
        'api-reference/index',
        'observability/index',
      ],
    },
    {
      type: 'category',
      label: 'Community & Help',
      items: [
        'examples/index',
        'troubleshooting',
      ],
    },
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
      label: 'Incremental Tutorial',
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
