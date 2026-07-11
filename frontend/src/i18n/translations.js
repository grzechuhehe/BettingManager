import { useUISettings } from '../context/UISettingsContext';

const translations = {
    en: {
        'settings.title': 'Settings',
        'settings.subtitle': 'Appearance, language, and trading preferences.',
        'settings.appearance': 'Interface theme',
        'settings.theme.light': 'Light',
        'settings.theme.dark': 'Dark',
        'settings.language': 'Language',
        'settings.trading': 'Trading preferences',
        'settings.evThreshold': '+EV threshold (%)',
        'settings.evHint': 'Minimum expected value edge shown in the dashboard +EV table.',
        'settings.displayCurrency': 'Display currency',
        'settings.displayCurrencyHint': 'Dashboard totals and charts are converted to this currency using fixed rates from server config.',
        'settings.save': 'Save changes',
        'settings.saved': 'Settings saved.',
        'settings.saveFailed': 'Failed to save settings.',
        'bets.title': 'Bet History',
        'bets.positions': 'positions',
        'sharpe.title': 'Sharpe Ratio',
        'sharpe.tooltip': 'Risk-adjusted return: higher means more stable profit relative to outcome volatility. Values above 1 are generally considered good.',
        'picks.filter.selection': 'Filter selection',
        'picks.sort': 'Sort',
        'picks.sort.asc': 'Ascending',
        'picks.sort.desc': 'Descending',
        'picks.all': 'All',
        'profile.performance': 'Performance snapshot',
        'profile.security': 'Change password',
    },
};

export function t(language, key) {
    return translations.en[key] ?? key;
}

export function useT() {
    useUISettings();
    return (key) => t('en', key);
}
