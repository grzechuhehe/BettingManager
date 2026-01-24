import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDashboardStats } from '../api';
import AdvancedAnalytics from './AdvancedAnalytics';
import BettingHeatmap from './BettingHeatmap';

const StatCard = ({ title, value, subtext, colorClass = "text-gray-900" }) => (
    <div className="bg-white p-6 rounded-lg shadow-md border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-lg transition-shadow duration-300">
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
                // Pobieramy teraz WSZYSTKIE dane (podstawowe + zaawansowane) jednym strzałem
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
        const sign = num >= 0 ? "+" : "-"; // Plus explicit tylko dla dodatnich
        const displaySign = num < 0 ? "-" : ""; 
        // Mała poprawka formatowania: -$100.00 wygląda lepiej niż -$-100.00
        return num < 0 
            ? `-$${Math.abs(num).toFixed(2)}` 
            : `+$${Math.abs(num).toFixed(2)}`;
    };

    const getProfitColor = (val) => {
        if (!val) return "text-gray-900";
        return val >= 0 ? "text-green-600" : "text-red-600";
    };

    return (
        <div className="p-4 max-w-7xl mx-auto space-y-10">
            {/* Header */}
            <header className="flex justify-between items-end pb-6 border-b border-gray-200">
                <div>
                    <h2 className="text-3xl font-bold text-gray-900">Dashboard</h2>
                    <p className="text-gray-500 mt-1">Welcome back, {user?.username || 'Trader'}! Here is your market overview.</p>
                </div>
                <div className="text-right hidden sm:block">
                    <p className="text-sm text-gray-400">Current Session</p>
                    <p className="font-mono text-gray-700">{new Date().toLocaleDateString()}</p>
                </div>
            </header>
            
            {/* KPI Cards */}
            <section>
                {loading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
                        {[...Array(4)].map((_, i) => (
                            <div key={i} className="h-32 bg-gray-200 rounded-lg"></div>
                        ))}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                        <StatCard 
                            title="Total P/L" 
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
                            subtext="Open Positions"
                        />
                        <StatCard 
                            title="Total Volume" 
                            value={formatCurrency(stats?.totalStaked || 0).replace('+', '')} // Bez plusa dla wolumenu
                            subtext={`${stats?.totalBets || 0} Trades`}
                        />
                    </div>
                )}
            </section>

            {/* Advanced Analytics (Graphs & Charts) */}
            <section>
                <h3 className="text-xl font-semibold text-gray-800 mb-6 px-1 flex items-center gap-2">
                    <span>📊</span> Market Analysis
                </h3>
                {loading ? (
                    <div className="h-80 bg-gray-100 rounded-lg animate-pulse"></div>
                ) : (
                    <>
                        <AdvancedAnalytics stats={stats} />
                        <div className="mt-8">
                            <h4 className="text-lg font-medium text-gray-700 mb-4 px-1">Activity Heatmap</h4>
                            <BettingHeatmap />
                        </div>
                    </>
                )}
            </section>

            {/* Quick Actions */}
            <section className="bg-gray-50 p-6 rounded-xl border border-gray-200">
                <h3 className="text-xl font-semibold text-gray-800 mb-4 px-1">Quick Execution</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <ActionCard 
                        to="/add-bet"
                        title="Place Order"
                        description="Log a new bet manually."
                        icon="📝"
                    />
                    <ActionCard 
                        to="/bets"
                        title="Trade History"
                        description="View and settle open positions."
                        icon="📋"
                    />
                    <div className="flex flex-col items-center p-6 bg-white border border-dashed border-gray-300 rounded-lg justify-center text-center opacity-60 hover:opacity-100 transition-opacity cursor-pointer">
                         <div className="text-4xl mb-4">⚙️</div>
                         <h5 className="mb-1 text-lg font-semibold text-gray-600">Settings</h5>
                         <span className="text-xs bg-gray-100 text-gray-500 px-2 py-1 rounded-full">Coming Soon</span>
                    </div>
                </div>
            </section>
        </div>
    );
};

export default Dashboard;
