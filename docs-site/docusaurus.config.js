const {themes} = require('prism-react-renderer');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'FlowForge',
  tagline: 'The Code-First Reactive Workflow Engine for Java & Spring Boot',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://royada-labs.github.io',
  // Set the /<projectName>/ for GitHub Pages
  baseUrl: '/flowforge/',

  // GitHub pages config
  organizationName: 'royada-labs', // GitHub user/group name.
  projectName: 'flowforge', // repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          path: 'docs',
          sidebarPath: './sidebars.js',
          editUrl:
            'https://github.com/royada-labs/flowforge/tree/main/',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
      image: 'img/docusaurus-social-card.jpg',
      navbar: {
        title: 'FlowForge',
        logo: {
          alt: 'FlowForge Logo',
          src: 'img/flowforge-logo.png',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Docs',
          },
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Tutorial',
          },
          {
            href: 'https://github.com/royada-labs/flowforge',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Documentation',
                to: '/docs/getting-started',
              },
              {
                label: 'Tutorial',
                to: '/docs/tutorial',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'GitHub',
                href: 'https://github.com/royada-labs/flowforge',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} FlowForge Project. Built with Docusaurus.`,
      },
      prism: {
        theme: themes.github,
        darkTheme: themes.dracula,
        additionalLanguages: ['java', 'groovy'],
      },
    }),
};

module.exports = config;
