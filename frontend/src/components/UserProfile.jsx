import React, { useEffect, useState } from 'react';
import { getUserProfile, getDashboardStats, forgotPassword, updateUserSettings } from '../api';

const UserProfile = () => {
    const [profile, setProfile] = useState(null);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [resetEmail, setResetEmail] = useState('');
    const [isResetting, setIsResetting] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });
    const [isPasswordExpanded, setIsPasswordExpanded] = useState(false);
    
    const [evThreshold, setEvThreshold] = useState(2);
    const [isUpdatingSettings, setIsUpdatingSettings] = useState(false);
    const [settingsMessage, setSettingsSettingsMessage] = useState({ text: '', type: '' });

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
        } catch (error) {
            console.error("Error fetching profile data", error);
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateSettings = async (e) => {
        e.preventDefault();
        setSettingsSettingsMessage({ text: '', type: '' });
        setIsUpdatingSettings(true);
        try {
            await updateUserSettings({ evEdgeThreshold: parseInt(evThreshold) });
            setSettingsSettingsMessage({ text: 'Settings updated successfully.', type: 'success' });
        } catch (error) {
            setSettingsSettingsMessage({ text: 'Failed to update settings.', type: 'error' });
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
            setMessage({ text: response.data.message || 'Password reset email sent.', type: 'success' });
        } catch (error) {
            setMessage({ text: error.response?.data?.error || 'Failed to send reset link.', type: 'error' });
        } finally {
            setIsResetting(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });
    };

    if (loading) return (
        <div className="flex flex-col items-center justify-center py-40 gap-6">
            <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
            <p className="text-muted font-bold uppercase tracking-[0.2em] ">Syncing User Data...</p>
        </div>
    );

    return (
        <div className="max-w-7xl mx-auto space-y-12">
            <header className="pb-8 border-b border-hairline">
                <h2 className="display-md">User Settings</h2>
                <p className="text-body mt-2">Manage your identity and security parameters.</p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
                {/* Left Column: User Info & Stats */}
                <div className="md:col-span-1 space-y-8">
                    {/* Profile Card */}
                    <div className="bg-surface-card p-10 rounded-lg border border-hairline text-center">
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
                                        <span key={role} className="badge-pill">
                                            {role}
                                        </span>
                                    )) || <span className="bg-hairline text-muted text-[10px] px-2 py-0.5 rounded">User</span>}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Mini Stats Summary */}
                    <div className="bg-surface-card p-10 rounded-lg border border-hairline">
                        <h4 className="text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-8">Performance Snapshot</h4>
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
                                <span className={`text-xl font-bold leading-none font-numeric ${stats?.totalProfitLoss >= 0 ? 'text-primary' : 'text-accent-rose'}`}>
                                    ${stats?.totalProfitLoss?.toFixed(2) || '0.00'}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column: Settings & Security */}
                <div className="md:col-span-2 space-y-10">
                    {/* Change Password Card (Collapsible) */}
                    <div className="bg-surface-card rounded-lg border border-hairline overflow-hidden">
                        <button 
                            onClick={() => setIsPasswordExpanded(!isPasswordExpanded)}
                            className="w-full flex justify-between items-center p-8 bg-surface-card hover:bg-surface-soft transition-colors text-left"
                        >
                             <div className="flex items-center gap-6">
                                <span className="text-2xl w-12 h-12 bg-surface-elevated flex items-center justify-center rounded-lg border border-hairline">🔒</span>
                                <div>
                                    <h3 className="text-xl font-bold text-on-dark">Change Password</h3>
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
                                            Enter your email address and we will send you a link to reset it safely.
                                        </p>
                                        <label className="field-label">Email address</label>
                                        <input
                                            type="email"
                                            required
                                            className="input-field max-w-md"
                                            value={resetEmail}
                                            onChange={(e) => setResetEmail(e.target.value)}
                                        />
                                    </div>

                                    {message.text && (
                                        <div className={`p-4 rounded-lg border text-[10px] font-bold uppercase tracking-widest ${message.type === 'error' ? 'bg-accent-rose/10 border-accent-rose/50 text-accent-rose' : 'bg-accent-emerald/10 border-accent-emerald/50 text-accent-emerald'}`}>
                                            {message.text}
                                        </div>
                                    )}

                                    <div className="flex justify-start pt-4">
                                        <button
                                            type="submit"
                                            disabled={isResetting}
                                            className="button-primary disabled:opacity-50"
                                        >
                                            {isResetting ? 'Sending…' : 'Send reset link'}
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
