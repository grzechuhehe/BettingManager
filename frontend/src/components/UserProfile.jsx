import React, { useEffect, useState } from 'react';
import { getUserProfile, getDashboardStats, forgotPassword, updateUserSettings } from '../api';
import { formatSignedMoney } from '../utils/currency';
import { useUISettings } from '../context/UISettingsContext';
import { useT } from '../i18n/translations';
import ThemeToggle from './ui/ThemeToggle';

const UserProfile = () => {
    const { language, setLanguage } = useUISettings();
    const translate = useT();

    const [profile, setProfile] = useState(null);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [resetEmail, setResetEmail] = useState('');
    const [isResetting, setIsResetting] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });
    const [isPasswordExpanded, setIsPasswordExpanded] = useState(false);

    const [evThreshold, setEvThreshold] = useState(2);
    const [displayCurrency, setDisplayCurrency] = useState('PLN');
    const [isUpdatingSettings, setIsUpdatingSettings] = useState(false);
    const [settingsMessage, setSettingsMessage] = useState({ text: '', type: '' });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [profileRes, statsRes] = await Promise.all([
                getUserProfile(),
                getDashboardStats()
            ]);
            setProfile(profileRes.data);
            setStats(statsRes.data);
            setResetEmail(profileRes.data.email || '');
            setEvThreshold(profileRes.data.evEdgeThreshold || 2);
            setDisplayCurrency(profileRes.data.displayCurrency || 'PLN');
        } catch (error) {
            console.error("Error fetching profile data", error);
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateSettings = async (e) => {
        e.preventDefault();
        setSettingsMessage({ text: '', type: '' });
        setIsUpdatingSettings(true);
        try {
            await updateUserSettings({
                evEdgeThreshold: parseInt(evThreshold, 10),
                displayCurrency,
            });
            setSettingsMessage({ text: translate('settings.saved'), type: 'success' });
        } catch (error) {
            setSettingsMessage({ text: translate('settings.saveFailed'), type: 'error' });
        } finally {
            setIsUpdatingSettings(false);
        }
    };

    const handlePasswordResetRequest = async (e) => {
        e.preventDefault();
        setMessage({ text: '', type: '' });
        setIsResetting(true);

        try {
            const response = await forgotPassword(resetEmail);
            setMessage({ text: response.data.message || "Wysłano e-mail z linkiem do resetowania hasła.", type: 'success' });
        } catch (error) {
            setMessage({ text: error.response?.data?.error || "Nie udało się wysłać linku resetującego.", type: 'error' });
        } finally {
            setIsResetting(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleDateString(language === 'pl' ? 'pl-PL' : 'en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });
    };

    if (loading) return (
        <div className="flex flex-col items-center justify-center py-40 gap-6">
            <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
            <p className="text-muted font-bold uppercase tracking-[0.2em] animate-pulse">Syncing User Data...</p>
        </div>
    );

    return (
        <div className="max-w-7xl mx-auto space-y-12">
            <header className="pb-8 border-b border-hairline">
                <h2 className="display-md">{translate('settings.title')}</h2>
                <p className="text-body mt-2">{translate('settings.subtitle')}</p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
                <div className="md:col-span-1 space-y-8">
                    <div className="bg-surface-card p-10 rounded-lg border border-hairline text-center shadow-sm">
                        <div className="w-24 h-24 bg-primary text-on-primary rounded-full mx-auto flex items-center justify-center text-4xl font-black mb-6">
                            {profile?.username?.charAt(0).toUpperCase()}
                        </div>
                        <h3 className="text-2xl font-bold text-on-dark mb-1">{profile?.username}</h3>
                        <p className="text-muted text-sm mb-8">{profile?.email}</p>

                        <div className="border-t border-hairline pt-6 mt-6 text-left space-y-4">
                            <div>
                                <p className="text-[10px] font-black text-muted uppercase tracking-widest mb-1">Provisioned</p>
                                <p className="text-body-strong font-bold text-sm font-numeric">{formatDate(profile?.joinedAt)}</p>
                            </div>
                            <div>
                                <p className="text-[10px] font-black text-muted uppercase tracking-widest mb-1">Authorization</p>
                                <div className="flex flex-wrap gap-2 mt-2">
                                    {profile?.roles?.map(role => (
                                        <span key={role} className="inline-block bg-primary/10 text-primary border border-primary/30 text-[10px] font-black px-2 py-0.5 rounded-full uppercase tracking-tighter">
                                            {role}
                                        </span>
                                    )) || <span className="bg-hairline text-muted text-[10px] px-2 py-0.5 rounded">User</span>}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="bg-surface-card p-10 rounded-lg border border-hairline">
                        <h4 className="text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-8">{translate('profile.performance')}</h4>
                        <div className="space-y-6">
                            <div className="flex justify-between items-end border-b border-hairline pb-4">
                                <span className="text-xs font-bold text-muted uppercase tracking-widest">Total Positions</span>
                                <span className="text-xl font-bold text-on-dark leading-none font-numeric">{stats?.totalBets || 0}</span>
                            </div>
                            <div className="flex justify-between items-end border-b border-hairline pb-4">
                                <span className="text-xs font-bold text-muted uppercase tracking-widest">Success Rate</span>
                                <span className="text-xl font-bold text-primary leading-none font-numeric">{stats?.winRate?.toFixed(1) || 0}%</span>
                            </div>
                            <div className="flex justify-between items-end">
                                <span className="text-xs font-bold text-muted uppercase tracking-widest">Net Yield</span>
                                <span className={`text-xl font-bold leading-none font-numeric ${stats?.totalProfitLoss >= 0 ? 'text-primary' : 'text-rose-500'}`}>
                                    {formatSignedMoney(stats?.totalProfitLoss || 0, stats?.displayCurrency || displayCurrency)}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="md:col-span-2 space-y-10">
                    <div className="bg-surface-card rounded-lg border border-hairline overflow-hidden">
                        <div className="p-8 border-b border-hairline bg-surface-soft/30">
                            <h3 className="text-xl font-bold text-on-dark">{translate('settings.appearance')}</h3>
                        </div>
                        <div className="p-8 space-y-8">
                            <div>
                                <label className="block text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-3">
                                    {translate('settings.appearance')}
                                </label>
                                <ThemeToggle />
                            </div>
                            <div>
                                <label className="block text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-3">
                                    {translate('settings.language')}
                                </label>
                                <select
                                    value={language}
                                    onChange={(e) => setLanguage(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="pl">Polski (PL)</option>
                                    <option value="en">English (EN)</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <form onSubmit={handleUpdateSettings} className="bg-surface-card rounded-lg border border-hairline overflow-hidden">
                        <div className="p-8 border-b border-hairline bg-surface-soft/30">
                            <h3 className="text-xl font-bold text-on-dark">{translate('settings.trading')}</h3>
                        </div>
                        <div className="p-8 space-y-4">
                            <label className="block text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-3">
                                {translate('settings.evThreshold')}
                            </label>
                            <input
                                type="number"
                                min="0"
                                max="50"
                                value={evThreshold}
                                onChange={(e) => setEvThreshold(e.target.value)}
                                className="input-field max-w-xs"
                            />
                            <p className="text-xs text-muted">{translate('settings.evHint')}</p>
                            <label className="block text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-3 mt-6">
                                {translate('settings.displayCurrency')}
                            </label>
                            <select
                                value={displayCurrency}
                                onChange={(e) => setDisplayCurrency(e.target.value)}
                                className="input-field max-w-xs"
                            >
                                <option value="PLN">PLN</option>
                                <option value="USD">USD</option>
                                <option value="EUR">EUR</option>
                                <option value="GBP">GBP</option>
                            </select>
                            <p className="text-xs text-muted">{translate('settings.displayCurrencyHint')}</p>
                            {settingsMessage.text && (
                                <div className={`p-4 rounded-lg border text-[10px] font-bold uppercase tracking-widest ${settingsMessage.type === 'error' ? 'bg-rose-500/10 border-rose-500/50 text-rose-500' : 'bg-primary/10 border-primary/50 text-primary'}`}>
                                    {settingsMessage.text}
                                </div>
                            )}
                            <button
                                type="submit"
                                disabled={isUpdatingSettings}
                                className="button-primary text-sm disabled:opacity-50"
                            >
                                {translate('settings.save')}
                            </button>
                        </div>
                    </form>

                    <div className="bg-surface-card rounded-lg border border-hairline overflow-hidden">
                        <button
                            type="button"
                            onClick={() => setIsPasswordExpanded(!isPasswordExpanded)}
                            className="w-full flex justify-between items-center p-8 bg-surface-card hover:bg-surface-soft transition-colors text-left"
                        >
                            <div className="flex items-center gap-6">
                                <span className="text-2xl w-12 h-12 bg-surface-elevated flex items-center justify-center rounded-lg border border-hairline">🔒</span>
                                <div>
                                    <h3 className="text-xl font-bold text-on-dark">{translate('profile.security')}</h3>
                                    <p className="text-sm text-muted mt-1 font-medium">Update your account password.</p>
                                </div>
                            </div>
                            <span className={`transform transition-transform duration-300 text-primary font-bold ${isPasswordExpanded ? 'rotate-180' : ''}`}>
                                ↓
                            </span>
                        </button>

                        {isPasswordExpanded && (
                            <div className="p-10 border-t border-hairline bg-surface-soft/30">
                                <form onSubmit={handlePasswordResetRequest} className="space-y-8 max-w-2xl">
                                    <div>
                                        <p className="text-sm text-body mb-6">
                                            For security, changing your password requires verification.
                                            Enter your email address and we will send you a secure reset link.
                                        </p>
                                        <label className="block text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-3">Email Address</label>
                                        <input
                                            type="email"
                                            required
                                            className="input-field max-w-md"
                                            value={resetEmail}
                                            onChange={(e) => setResetEmail(e.target.value)}
                                        />
                                    </div>

                                    {message.text && (
                                        <div className={`p-4 rounded-lg border text-[10px] font-bold uppercase tracking-widest ${message.type === 'error' ? 'bg-rose-500/10 border-rose-500/50 text-rose-500' : 'bg-primary/10 border-primary/50 text-primary'}`}>
                                            {message.text}
                                        </div>
                                    )}

                                    <div className="flex justify-start pt-4">
                                        <button
                                            type="submit"
                                            disabled={isResetting}
                                            className="button-primary !h-12 !px-10 text-sm uppercase tracking-widest font-black disabled:opacity-50"
                                        >
                                            {isResetting ? 'Sending...' : 'Send reset link'}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default UserProfile;
