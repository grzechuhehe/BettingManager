import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDashboardStats } from '../api';
import AdvancedStats from './AdvancedStats';
import BettingHeatmap from './BettingHeatmap';

const StatCard = ({ title, value, subtext, colorClass = "text-gray-900" }) => (
    <div className="bg-white p-6 rounded-lg shadow-md border border-gray-100 flex flex-col items-center justify-center text-center">
        <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-2">{title}</h3>
        <p className={`text-3xl font-bold ${colorClass}`}>{value}</p>
        {subtext && <p className="text-xs text-gray-400 mt-1">{subtext}</p>}
    </div>
);

const ActionCard = ({ to, title, description, icon }) => (
    <Link to={to} className="flex flex-col items-center p-6 bg-white border border-gray-200 rounded-lg shadow-sm hover:shadow-md hover:bg-gray-50 transition-all duration-300 group">
        <div className="text-4xl mb-4 group-hover:scale-110 transition-transform">{icon}</div>
        <h5 className="mb-2 text-xl font-bold tracking-tight text-gray-900">{title}</h5>
        <p className="font-normal text-gray-600 text-center text-sm">{description}</p>
    </Link>
);

const Dashboard = () => {
    const { user } = useAuth();
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const response = await getDashboardStats();
                setStats(response.data);
            } catch (error) {
                console.error("Failed to fetch dashboard stats:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchStats();
    }, []);

    const formatCurrency = (val) => {
        if (val === null || val === undefined) return "$0.00";
        const num = parseFloat(val);
        const sign = num >= 0 ? "+" : "-";
        return `${sign}$${Math.abs(num).toFixed(2)}`;
    };

    const getProfitColor = (val) => {
        if (!val) return "text-gray-900";
        return val >= 0 ? "text-green-600" : "text-red-600";
    };

    return (
        <div className="p-4 max-w-6xl mx-auto">
            <header className="mb-8">
                <h2 className="text-3xl font-bold text-gray-800">Hello, {user || 'Bettor'}! 👋</h2>
                <p className="text-gray-500 mt-1">Here is what's happening with your bets today.</p>
            </header>
            
            {/* Stats Grid */}
            <section className="mb-10">
                <h3 className="text-xl font-semibold text-gray-800 mb-4 px-1">Performance Overview</h3>
                {loading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
                        {[...Array(4)].map((_, i) => (
                            <div key={i} className="h-32 bg-gray-200 rounded-lg"></div>
                        ))}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                        <StatCard 
                            title="Total Profit/Loss" 
                            value={formatCurrency(stats?.totalProfitLoss)} 
                            colorClass={getProfitColor(stats?.totalProfitLoss)}
                        />
                        <StatCard 
                            title="Win Rate" 
                            value={stats?.winRate ? `${stats.winRate.toFixed(1)}%` : "0%"} 
                            colorClass="text-blue-600"
                        />
                        <StatCard 
                            title="Active Bets" 
                            value={stats?.activeBetsCount || 0} 
                            subtext="Pending Settlement"
                        />
                        <StatCard 
                            title="Total Bets" 
                            value={stats?.totalBets || 0} 
                            subtext="Lifetime"
                        />
                    </div>
                )}
            </section>

            {/* Advanced Analytics Section */}
            <section className="mb-10">
                <h3 className="text-xl font-semibold text-gray-800 mb-4 px-1">Detailed Analytics</h3>
                <AdvancedStats />
                <div className="mt-6">
                    <BettingHeatmap />
                </div>
            </section>

            {/* Quick Actions */}
            <section>
                <h3 className="text-xl font-semibold text-gray-800 mb-4 px-1">Quick Actions</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <ActionCard 
                        to="/add-bet"
                        title="Place New Bet"
                        description="Record a new wager quickly."
                        icon="📝"
                    />
                    <ActionCard 
                        to="/bets"
                        title="My Bets"
                        description="View history and settle pending bets."
                        icon="📊"
                    />
                    {/* Placeholder for future features like 'Analysis' or 'Profile' */}
                    <div className="flex flex-col items-center p-6 bg-gray-50 border border-dashed border-gray-300 rounded-lg justify-center text-center opacity-70">
                         <div className="text-4xl mb-4 grayscale">👤</div>
                         <h5 className="mb-1 text-lg font-semibold text-gray-500">User Profile</h5>
                         <span className="text-xs bg-gray-200 text-gray-600 px-2 py-1 rounded-full">Coming Soon</span>
                    </div>
                </div>
            </section>
        </div>
    );
};

export default Dashboard;
