import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDashboardStats } from '../api';
import AdvancedAnalytics from './AdvancedAnalytics';
import AdvancedStats from './AdvancedStats';
import BettingHeatmap from './BettingHeatmap';

const StatCard = ({ title, value, subtext, colorClass = "text-primary" }) => (
    <div className="bg-surface-card p-8 rounded-lg border border-hairline flex flex-col transition-all duration-300">
        <h3 className="text-xs font-semibold text-muted uppercase tracking-widest mb-4">{title}</h3>
        <p className={`stat-display ${colorClass}`}>{value}</p>
        {subtext && <p className="text-sm text-muted mt-2">{subtext}</p>}
    </div>
);

const ActionCard = ({ to, title, description, icon }) => (
    <Link to={to} className="flex flex-col p-8 bg-surface-card border border-hairline rounded-lg hover:border-primary/50 transition-all duration-300 group">
        <div className="text-3xl mb-6 group-hover:scale-110 transition-transform">{icon}</div>
        <h5 className="mb-2 text-xl font-bold tracking-tight text-on-dark">{title}</h5>
        <p className="font-normal text-muted text-sm leading-relaxed">{description}</p>
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
        return num < 0 
            ? `-$${Math.abs(num).toFixed(2)}` 
            : `+$${Math.abs(num).toFixed(2)}`;
    };

    const getProfitColor = (val) => {
        if (!val) return "text-primary";
        return val >= 0 ? "text-primary" : "text-rose-500";
    };

    return (
        <div className="space-y-24">
            <header className="flex justify-between items-end pb-8 border-b border-hairline">
                <div>
                    <h2 className="display-md">Dashboard</h2>
                    <p className="text-body mt-2">Welcome back, <span className="text-primary font-bold">{user || 'Trader'}</span>! Here is your market overview.</p>
                </div>
                <div className="text-right hidden sm:block">
                    <p className="text-xs font-semibold text-muted uppercase tracking-widest">Market Date</p>
                    <p className="font-mono text-on-dark mt-1 font-numeric">{new Date().toLocaleDateString()}</p>
                </div>
            </header>
            
            <section>
                {loading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 animate-pulse">
                        {[...Array(4)].map((_, i) => (
                            <div key={i} className="h-40 bg-surface-card rounded-lg border border-hairline"></div>
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
                        />
                        <StatCard 
                            title="Active Bets" 
                            value={stats?.activeBetsCount || 0} 
                            subtext="Open Positions"
                        />
                        <StatCard 
                            title="Total Volume" 
                            value={formatCurrency(stats?.totalStaked || 0).replace('+', '')}
                            subtext={`${stats?.totalBets || 0} Trades`}
                        />
                    </div>
                )}
            </section>

            <section>
                <div className="flex items-center gap-3 mb-10">
                    <div className="w-1.5 h-6 bg-primary rounded-full"></div>
                    <h3 className="text-xl font-bold text-on-dark">Market Analysis</h3>
                </div>
                {loading ? (
                    <div className="h-80 bg-surface-card rounded-lg border border-hairline animate-pulse"></div>
                ) : (
                    <div className="space-y-12">
                        <AdvancedStats />
                        <AdvancedAnalytics stats={stats} />
                        <div className="bg-surface-card border border-hairline rounded-lg p-10">
                            <h4 className="text-lg font-bold text-on-dark mb-8">Activity Heatmap</h4>
                            <BettingHeatmap />
                        </div>
                    </div>
                )}
            </section>

            <section className="bg-surface-soft p-12 rounded-xl border border-hairline">
                <h3 className="text-xl font-bold text-on-dark mb-10">Quick Execution</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    <ActionCard 
                        to="/add-bet"
                        title="Place Order"
                        description="Log a new betting position with detailed market parameters."
                        icon="📝"
                    />
                    <ActionCard 
                        to="/ev-calculator"
                        title="EV Calculator"
                        description="Compare bookmaker odds against true probability from prediction markets."
                        icon="⚖️"
                    />
                    <ActionCard 
                        to="/bets"
                        title="Trade History"
                        description="Audit and settle all open and historical market positions."
                        icon="📋"
                    />
                </div>
            </section>
        </div>
    );
};

export default Dashboard;
