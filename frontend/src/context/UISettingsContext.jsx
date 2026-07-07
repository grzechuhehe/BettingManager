import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

const STORAGE_THEME = 'ui-theme';
const STORAGE_LANG = 'ui-language';

const UISettingsContext = createContext(null);

export function UISettingsProvider({ children }) {
    const [theme, setThemeState] = useState(() => localStorage.getItem(STORAGE_THEME) || 'dark');
    const [language, setLanguageState] = useState(() => localStorage.getItem(STORAGE_LANG) || 'en');

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem(STORAGE_THEME, theme);
    }, [theme]);

    useEffect(() => {
        localStorage.setItem(STORAGE_LANG, language);
    }, [language]);

    const value = useMemo(() => ({
        theme,
        language,
        setTheme: setThemeState,
        setLanguage: setLanguageState,
        isDark: theme === 'dark',
    }), [theme, language]);

    return (
        <UISettingsContext.Provider value={value}>
            {children}
        </UISettingsContext.Provider>
    );
}

export function useUISettings() {
    const ctx = useContext(UISettingsContext);
    if (!ctx) {
        throw new Error('useUISettings must be used within UISettingsProvider');
    }
    return ctx;
}
