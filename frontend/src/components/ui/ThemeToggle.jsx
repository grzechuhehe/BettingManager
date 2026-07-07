import React from 'react';
import { useUISettings } from '../../context/UISettingsContext';
import { t } from '../../i18n/translations';

export default function ThemeToggle() {
    const { theme, setTheme, language } = useUISettings();

    const options = [
        { value: 'light', label: t(language, 'settings.theme.light'), icon: '☀️' },
        { value: 'dark', label: t(language, 'settings.theme.dark'), icon: '🌙' },
    ];

    return (
        <div className="grid grid-cols-2 gap-2 p-1 bg-canvas border border-hairline rounded-lg">
            {options.map((opt) => (
                <button
                    key={opt.value}
                    type="button"
                    onClick={() => setTheme(opt.value)}
                    className={`flex items-center justify-center gap-2 py-3 rounded-md text-sm font-semibold transition-all ${
                        theme === opt.value
                            ? 'bg-primary text-on-primary shadow-lg shadow-primary/10'
                            : 'text-muted hover:text-on-dark'
                    }`}
                >
                    <span>{opt.icon}</span>
                    <span>{opt.label}</span>
                </button>
            ))}
        </div>
    );
}
