import React, { useEffect, useState } from 'react';
import { getUserProfile, changePassword, getDashboardStats } from '../api';

const UserProfile = () => {
    const [profile, setProfile] = useState(null);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [passwords, setPasswords] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
    const [message, setMessage] = useState({ text: '', type: '' });
    const [isPasswordExpanded, setIsPasswordExpanded] = useState(false);

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
        } catch (error) {
            console.error("Error fetching profile data", error);
        } finally {
            setLoading(false);
        }
    };

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        setMessage({ text: '', type: '' });

        if (passwords.newPassword !== passwords.confirmPassword) {
            setMessage({ text: "New passwords do not match!", type: 'error' });
            return;
        }
        if (passwords.newPassword.length < 6) {
            setMessage({ text: "Password must be at least 6 characters.", type: 'error' });
            return;
        }

        try {
            await changePassword({ 
                oldPassword: passwords.oldPassword, 
                newPassword: passwords.newPassword 
            });
            setMessage({ text: "Password changed successfully!", type: 'success' });
            setPasswords({ oldPassword: '', newPassword: '', confirmPassword: '' });
        } catch (error) {
            setMessage({ text: error.response?.data || "Failed to change password", type: 'error' });
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });
    };

    if (loading) return <div className="text-center mt-10 text-gray-500">Loading Profile...</div>;

    return (
        <div className="max-w-4xl mx-auto p-6">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">User Profile</h2>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                {/* Left Column: User Info & Stats */}
                <div className="md:col-span-1 space-y-6">
                    {/* Profile Card */}
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 text-center">
                        <div className="w-24 h-24 bg-indigo-100 rounded-full mx-auto flex items-center justify-center text-indigo-600 text-3xl font-bold mb-4">
                            {profile?.username?.charAt(0).toUpperCase()}
                        </div>
                        <h3 className="text-xl font-bold text-gray-900">{profile?.username}</h3>
                        <p className="text-gray-500 text-sm mb-4">{profile?.email}</p>
                        
                        <div className="border-t border-gray-100 pt-4 mt-4 text-left">
                            <p className="text-xs text-gray-400 uppercase font-semibold">Joined</p>
                            <p className="text-gray-700 font-medium">{formatDate(profile?.joinedAt)}</p>
                        </div>
                        <div className="mt-2 text-left">
                            <p className="text-xs text-gray-400 uppercase font-semibold">Role</p>
                            <span className="inline-block bg-indigo-50 text-indigo-700 text-xs px-2 py-1 rounded mt-1">
                                {profile?.roles?.join(', ') || 'User'}
                            </span>
                        </div>
                    </div>

                    {/* Mini Stats Summary */}
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                        <h4 className="text-sm font-bold text-gray-500 uppercase mb-4">Quick Stats</h4>
                        <div className="space-y-3">
                            <div className="flex justify-between">
                                <span className="text-gray-600">Total Bets</span>
                                <span className="font-bold text-gray-900">{stats?.totalBets || 0}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-600">Win Rate</span>
                                <span className="font-bold text-blue-600">{stats?.winRate?.toFixed(1) || 0}%</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-600">Profit</span>
                                <span className={`font-bold ${stats?.totalProfitLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                    ${stats?.totalProfitLoss?.toFixed(2) || '0.00'}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column: Change Password (Collapsible) */}
                <div className="md:col-span-2">
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                        <button 
                            onClick={() => setIsPasswordExpanded(!isPasswordExpanded)}
                            className="w-full flex justify-between items-center p-6 bg-white hover:bg-gray-50 transition-colors text-left"
                        >
                            <div className="flex items-center gap-3">
                                <span className="text-xl bg-gray-100 p-2 rounded-full">🔒</span>
                                <div>
                                    <h3 className="text-xl font-bold text-gray-800">Change Password</h3>
                                    <p className="text-sm text-gray-500">Update your security credentials</p>
                                </div>
                            </div>
                            <span className={`transform transition-transform duration-300 text-gray-400 ${isPasswordExpanded ? 'rotate-180' : ''}`}>
                                ▼
                            </span>
                        </button>
                        
                        {isPasswordExpanded && (
                            <div className="p-8 border-t border-gray-100">
                                <form onSubmit={handlePasswordChange} className="space-y-5">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
                                        <input
                                            type="password"
                                            required
                                            className="w-full p-2 border border-gray-300 rounded focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                                            value={passwords.oldPassword}
                                            onChange={(e) => setPasswords({...passwords, oldPassword: e.target.value})}
                                        />
                                    </div>
                                    
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                                            <input
                                                type="password"
                                                required
                                                className="w-full p-2 border border-gray-300 rounded focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                                                value={passwords.newPassword}
                                                onChange={(e) => setPasswords({...passwords, newPassword: e.target.value})}
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
                                            <input
                                                type="password"
                                                required
                                                className="w-full p-2 border border-gray-300 rounded focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                                                value={passwords.confirmPassword}
                                                onChange={(e) => setPasswords({...passwords, confirmPassword: e.target.value})}
                                            />
                                        </div>
                                    </div>

                                    {message.text && (
                                        <div className={`p-3 rounded text-sm ${message.type === 'error' ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'}`}>
                                            {message.text}
                                        </div>
                                    )}

                                    <div className="flex justify-end pt-2">
                                        <button
                                            type="submit"
                                            className="px-6 py-2 bg-indigo-600 text-white font-medium rounded hover:bg-indigo-700 transition-colors shadow-sm"
                                        >
                                            Update Password
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