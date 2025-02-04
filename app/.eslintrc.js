module.exports = {
  parser: '@babel/eslint-parser',
  parserOptions: {
    ecmaVersion: 2020,
    requireConfigFile: false,
  },
  env: {
    browser: true,
    node: true,
  },
  rules: {
    semi: ['error', 'always'],
    quotes: ['error', 'single'],
    'default-param-last': 'off',
    'arrow-body-style': 'off',
    'import/no-named-as-default': 'off',
    'import/no-named-as-default-member': 'off',
    'react/button-has-type': 'off',
    'jsx-a11y/click-events-have-key-events': 'off',
    'jsx-a11y/no-static-element-interactions': 'off',
    'react/no-array-index-key': 'off',
    'react-hooks/exhaustive-deps': 'off',
    'jsx-a11y/label-has-associated-control': 'off',
    'react/jsx-filename-extension': 'off',
    'jsx-a11y/no-noninteractive-element-interactions': 'off',
    'import/no-extraneous-dependencies': 'off',
    'import/no-cycle': 'off',
  },
  extends: ['react-app', 'airbnb', 'prettier', 'plugin:storybook/recommended'],
  plugins: ['react', 'prettier'],
  settings: {
    'import/resolver': {
      alias: {
        map: [
          ['@/', './src/'],
          ['@/images', './src/images'],
          ['@/vendor', './src/vendor'],
          ['@/assets', './src/assets'],
          ['@/components', './src/components'],
          ['@/pages', './src/pages'],
          ['@/stores', './src/stores'],
          ['@/utils', './src/utils'],
          ['@/constants', './src/constants'],
          ['@/proptypes', './src/proptypes'],
          ['@/services', './src/services'],
          ['@/hooks', './src/hooks'],
          ['@/styles', './src/styles'],
          ['@/configurations', './src/configurations'],
        ],
        extensions: ['.ts', '.js', '.jsx', '.json'],
      },
    },
  },
};
