import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDashboardStats, getEvOpportunities } from '../api';
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
    const [opportunities, setOpportunities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expandedEvents, setExpandedEvents] = useState({});

    const toggleEvent = (eventName) => {
        setExpandedEvents(prev => ({
            ...prev,
            [eventName]: !prev[eventName]
        }));
    };

    // Grouping logic
    const grouped = opportunities.reduce((acc, opp) => {
        if (!acc[opp.eventName]) acc[opp.eventName] = [];
        acc[opp.eventName].push(opp);
        return acc;
    }, {});

    // Sort events by the highest EV opportunity within each
    const sortedEventNames = Object.keys(grouped).sort((a, b) => {
        const maxEvA = Math.max(...grouped[a].map(o => o.evPercentage));
        const maxEvB = Math.max(...grouped[b].map(o => o.evPercentage));
        return maxEvB - maxEvA;
    });

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

        const fetchOpportunities = async () => {
            try {
                const response = await getEvOpportunities();
                setOpportunities(response.data);
            } catch (error) {
                console.error("Failed to fetch EV opportunities:", error);
            }
        };

        fetchStats();
        fetchOpportunities();
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

            <section className="mt-12">
                <div className="flex items-center gap-3 mb-10">
                    <div className="w-1.5 h-6 bg-primary rounded-full animate-pulse"></div>
                    <h3 className="text-xl font-bold text-on-dark">Live +EV Opportunities</h3>
                </div>
                <div className="bg-surface-card border border-hairline rounded-lg overflow-hidden">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-surface-soft border-b border-hairline">
                                <th className="p-4 text-[10px] font-black text-muted uppercase tracking-widest">Market / Event</th>
                                <th className="p-4 text-[10px] font-black text-muted uppercase tracking-widest">Selection</th>
                                <th className="p-4 text-[10px] font-black text-muted uppercase tracking-widest">Bookmaker</th>
                                <th className="p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">Odds</th>
                                <th className="p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">True Prob</th>
                                <th className="p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right text-primary">Expected Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            {sortedEventNames.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="p-10 text-center text-muted italic">Scanning markets for opportunities...</td>
                                </tr>
                            ) : (
                                sortedEventNames.map((eventName) => {
                                    const eventOpps = grouped[eventName];
                                    // eventOpps are already sorted by EV desc from backend query
                                    const bestOpp = eventOpps[0];
                                    const isExpanded = expandedEvents[eventName];

                                    return (
                                        <React.Fragment key={eventName}>
                                            <tr 
                                                className="border-b border-hairline hover:bg-surface-soft transition-colors cursor-pointer"
                                                onClick={() => toggleEvent(eventName)}
                                            >
                                                <td className="p-4 font-bold text-on-dark flex items-center gap-3">
                                                    <span className={`text-[10px] transition-transform duration-200 ${isExpanded ? 'rotate-90' : ''}`}>▶</span>
                                                    {eventName}
                                                </td>
                                                <td className="p-4 text-muted font-bold text-primary">{bestOpp.targetSelection}</td>
                                                <td className="p-4 text-muted">
                                                    {bestOpp.bookmaker}
                                                    {eventOpps.length > 1 && (
                                                        <span className="ml-2 px-2 py-0.5 bg-surface-soft text-[10px] rounded-full border border-hairline">
                                                            +{eventOpps.length - 1} more
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="p-4 text-right font-numeric font-bold">{bestOpp.bookmakerOdds.toFixed(2)}</td>
                                                <td className="p-4 text-right font-numeric text-muted">{(bestOpp.trueProbability * 100).toFixed(1)}%</td>
                                                <td className="p-4 text-right font-numeric font-black text-primary">+{bestOpp.evPercentage.toFixed(2)}%</td>
                                            </tr>
                                            {isExpanded && eventOpps.slice(1).map((opp) => (
                                                <tr key={opp.id} className="border-b border-hairline bg-surface-soft/30 text-sm">
                                                    <td className="p-4 pl-10 text-muted italic">↳ Alternative</td>
                                                    <td className="p-4 text-muted font-bold">{opp.targetSelection}</td>
                                                    <td className="p-4 text-muted">{opp.bookmaker}</td>
                                                    <td className="p-4 text-right font-numeric font-semibold">{opp.bookmakerOdds.toFixed(2)}</td>
                                                    <td className="p-4 text-right font-numeric text-muted">{(opp.trueProbability * 100).toFixed(1)}%</td>
                                                    <td className="p-4 text-right font-numeric font-bold text-primary/70">+{opp.evPercentage.toFixed(2)}%</td>
                                                </tr>
                                            ))}
                                        </React.Fragment>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
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
