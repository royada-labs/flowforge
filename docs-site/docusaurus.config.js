const {themes} = require('prism-react-renderer');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'FlowForge',
  tagline: 'Forge reactive workflows with precision',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://tu-usuario.gitlab.io',
  // Set the /<projectName>/ for GitLab Pages
  baseUrl: '/flowforge/',

  // GitLab pages config
  organizationName: 'tu-usuario', // Usually your GitLab user/group name.
  projectName: 'flowforge', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
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
          sidebarPath: './sidebars.js',
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://gitlab.com/tu-usuario/flowforge/-/tree/main/',
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
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Tutorial',
          },
          {
            href: 'https://gitlab.com/tu-usuario/flowforge',
            label: 'GitLab',
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
                label: 'Tutorial',
                to: '/docs/tutorial',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'GitLab',
                href: 'https://gitlab.com/tu-usuario/flowforge',
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
