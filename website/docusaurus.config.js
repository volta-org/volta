import {themes as prismThemes} from 'prism-react-renderer';

const simplePlantUML = require('@akebifiky/remark-simple-plantuml');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Volta',

  future: {
    v4: true,
  },

  url: 'https://volta-org.github.io',
  baseUrl: '/volta/',
  organizationName: 'volta-org',
  projectName: 'volta',
  onBrokenLinks: 'throw',
  trailingSlash: false,

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          remarkPlugins: [simplePlantUML],
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
    [
      'redocusaurus',
      {
        specs: [
          {
            id: 'volta',
            spec: 'api_specs/volta_ui_openapi.yaml',
          },
        ],
        theme: {
          primaryColor: '#1890ff',
        },
      }
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/docusaurus-social-card.jpg',
      colorMode: {
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'Volta',
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Docs',
          },
          {
            href: 'https://github.com/volta-org/volta',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
      },
    }),
};

export default config;
