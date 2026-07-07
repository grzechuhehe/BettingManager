import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDashboardStats, getEvOpportunities, getUserProfile, updateUserSettings } from '../api';
import AdvancedAnalytics from './AdvancedAnalytics';
import AdvancedStats from './AdvancedStats';
import BettingHeatmap from './BettingHeatmap';
import AnimatedNumber from './AnimatedNumber';

const StatCard = ({ title, value, subtext, colorClass = "text-primary", animateValue, formatValue }) => (
    <div className="bg-surface-card p-8 rounded-lg border border-hairline flex flex-col">
        <h3 className="table-header mb-4">{title}</h3>
        {animateValue != null ? (
            <AnimatedNumber
                value={animateValue}
                className={`stat-display ${colorClass}`}
                format={formatValue}
            />
        ) : (
            <p className={`stat-display ${colorClass}`}>{value}</p>
        )}
        {subtext && <p className="body-sm text-muted mt-2">{subtext}</p>}
    </div>
);

const ActionCard = ({ to, title, description, icon }) => (
    <Link to={to} className="flex flex-col p-8 bg-surface-card border border-hairline rounded-lg ">
        <div className="text-3xl mb-6 ">{icon}</div>
        <h5 className="mb-2 text-xl font-bold tracking-tight text-on-dark">{title}</h5>
        <p className="font-normal text-muted text-sm leading-relaxed">{description}</p>
    </Link>
);

const Dashboard = () => {
    const { user } = useAuth();
    const [stats, setStats] = useState(null);
    const [opportunities, setOpportunities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isUpdating, setIsUpdating] = useState(false);
    const [expandedEvents, setExpandedEvents] = useState({});
    const [showAsProbability, setShowAsProbability] = useState(false);
    const [minEdge, setMinEdge] = useState(2);

    const toggleEvent = (eventName) => {
        setExpandedEvents(prev => ({
            ...prev,
            [eventName]: !prev[eventName]
        }));
    };

    const getImpliedProb = (odds) => ((1 / odds) * 100).toFixed(1) + '%';

    const fetchOpportunities = async () => {
        setIsUpdating(true);
        try {
            const response = await getEvOpportunities();
            setOpportunities(response.data || []);
        } catch (error) {
            console.error("Failed to fetch EV opportunities:", error);
        } finally {
            setIsUpdating(false);
        }
    };

    // Debounce effect for edge threshold
    useEffect(() => {
        if (loading) return;

        const timer = setTimeout(async () => {
            try {
                await updateUserSettings({ evEdgeThreshold: minEdge });
                await fetchOpportunities();
            } catch (error) {
                console.error("Failed to update edge threshold:", error);
            }
        }, 800);

        return () => clearTimeout(timer);
    }, [minEdge]);

    useEffect(() => {
        const fetchInitialData = async () => {
            setLoading(true);
            try {
                const [statsRes, profileRes] = await Promise.all([
                    getDashboardStats(),
                    getUserProfile()
                ]);
                setStats(statsRes.data);
                if (profileRes.data.evEdgeThreshold !== undefined) {
                    setMinEdge(profileRes.data.evEdgeThreshold);
                }
            } catch (error) {
                console.error("Failed to fetch initial dashboard data:", error);
            } finally {
                setLoading(false);
                fetchOpportunities();
            }
        };

        fetchInitialData();
    }, []);

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

    const formatCurrency = (val) => {
        if (val === null || val === undefined) return "$0.00";
        const num = parseFloat(val);
        return num < 0 
            ? `-$${Math.abs(num).toFixed(2)}` 
            : `+$${Math.abs(num).toFixed(2)}`;
    };

    const getProfitColor = (val) => {
        if (!val) return "text-primary";
        return val >= 0 ? "text-primary" : "text-accent-rose";
    };

    return (
        <div className="space-y-24">
            <header className="flex justify-between items-end pb-8 border-b border-hairline">
                <div>
                    <h2 className="display-md">Dashboard</h2>
                    <p className="text-body mt-2">Welcome back, <span className="text-body-strong font-semibold">{user || 'Trader'}</span>! Here is your market overview.</p>
                </div>
                <div className="text-right hidden sm:block">
                    <p className="text-lg font-mono text-on-dark font-numeric tracking-tight">
                        {new Date().toLocaleDateString('en-US', {
                            month: 'long',
                            day: 'numeric',
                            year: 'numeric',
                        })}
                    </p>
                </div>
            </header>
            
            <section>
                {loading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 stagger-children">
                        {[...Array(4)].map((_, i) => (
                            <div key={i} className="h-40 bg-surface-card rounded-lg border border-hairline animate-skeleton"></div>
                        ))}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 stagger-children">
                        <StatCard 
                            title="Total P/L" 
                            value={formatCurrency(stats?.totalProfitLoss)} 
                            colorClass={getProfitColor(stats?.totalProfitLoss)}
                        />
                        <StatCard 
                            title="Win Rate" 
                            value={stats?.winRate ? `${stats.winRate.toFixed(1)}%` : "0%"}
                            animateValue={stats?.winRate ?? 0}
                            formatValue={(n) => `${n.toFixed(1)}%`}
                        />
                        <StatCard 
                            title="Active Bets" 
                            value={stats?.activeBetsCount || 0}
                            animateValue={stats?.activeBetsCount ?? 0}
                            formatValue={(n) => String(Math.round(n))}
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
                    <div className="h-80 bg-surface-card rounded-lg border border-hairline animate-skeleton"></div>
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
                <div className="flex items-center justify-between mb-10">
                    <div className="flex items-center gap-3">
                        <div className="w-1.5 h-6 bg-primary rounded-full animate-pulse"></div>
                        <h3 className="text-xl font-bold text-on-dark">Live +EV Opportunities</h3>
                    </div>
                    {isUpdating && (
                        <div className="flex items-center gap-2 text-[10px] font-black text-primary uppercase tracking-widest animate-pulse">
                            <div className="w-2 h-2 bg-primary rounded-full"></div>
                            Syncing Markets...
                        </div>
                    )}
                </div>
                <div className="bg-surface-card border border-hairline rounded-lg overflow-hidden">
                    <table className="w-full text-left border-collapse table-fixed">
                        <thead>
                            <tr className="bg-surface-soft border-b border-hairline">
                                <th className="w-1/3 p-4 text-[10px] font-black text-muted uppercase tracking-widest">Market / Event</th>
                                <th className="w-1/6 p-4 text-[10px] font-black text-muted uppercase tracking-widest">Selection</th>
                                <th className="w-1/6 p-4 text-[10px] font-black text-muted uppercase tracking-widest">Bookmaker</th>
                                <th className="w-1/12 p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right cursor-pointer hover:text-primary" onClick={() => setShowAsProbability(!showAsProbability)}>
                                    Odds {showAsProbability ? '(Prob)' : '(Decimal)'}
                                </th>
                                <th className="w-1/12 p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">True Prob</th>
                                <th className="w-1/12 p-4 text-[10px] font-black text-muted uppercase tracking-widest text-center text-primary">
                                    <div className="flex flex-col items-center">
                                        <span>Expected Value</span>
                                        <div className="mt-2 flex items-center gap-2 font-normal normal-case tracking-normal">
                                            <div className="relative group">
                                                <input 
                                                    type="number" 
                                                    min="0"
                                                    max="50"
                                                    className="w-14 bg-surface-elevated border border-hairline rounded px-2 py-1 text-xs text-primary font-black text-center focus:border-primary outline-none transition-all hover:border-primary/50"
                                                    value={minEdge}
                                                    onChange={(e) => setMinEdge(e.target.value)}
                                                />
                                                <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 p-3 bg-surface-elevated border border-hairline rounded text-[10px] text-muted opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-20 leading-relaxed">
                                                    Filter results. Table updates automatically <span className="text-primary font-bold">800ms</span> after your last change.
                                                </div>
                                            </div>
                                            <span className="text-[10px] text-muted-foreground/80 font-bold">%</span>
                                        </div>
                                    </div>
                                </th>
                            </tr>
                        </thead>
                        <tbody className={isUpdating ? 'opacity-50 transition-opacity' : 'transition-opacity'}>
                            {sortedEventNames.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="p-10 text-center text-muted italic">Scanning markets for opportunities...</td>
                                </tr>
                            ) : (
                                sortedEventNames.map((eventName) => {
                                    const eventOpps = grouped[eventName];
                                    const bestOpp = eventOpps[0];
                                    const isExpanded = expandedEvents[eventName];

                                    return (
                                        <React.Fragment key={eventName}>
                                            <tr 
                                                className="border-b border-hairline hover:bg-surface-soft transition-colors cursor-pointer"
                                                onClick={() => toggleEvent(eventName)}
                                            >
                                                <td className="p-4 font-bold text-on-dark flex items-center gap-3 h-16 truncate">
                                                    <span className={`text-[10px] transition-transform duration-200 ${isExpanded ? 'rotate-90' : ''}`}>▶</span>
                                                    <span className="truncate">{eventName}</span>
                                                    {(bestOpp.marketLiquidity === null || bestOpp.marketLiquidity < 20000) && (
                                                        <span 
                                                            className="text-warning cursor-help shrink-0" 
                                                            title={`Low liquidity ($${bestOpp.marketLiquidity?.toLocaleString('en-US', {maximumFractionDigits: 0}) || 0}). True probability might be inaccurate.`}
                                                        >
                                                            ⚠️
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="p-4 text-muted font-bold text-primary h-16 truncate">{bestOpp.targetSelection}</td>
                                                <td className="p-4 text-muted h-16">
                                                    <div className="flex flex-col justify-center h-full">
                                                        <span className="truncate">{bestOpp.bookmaker}</span>
                                                        <div className="flex gap-1 mt-0.5">
                                                            {bestOpp.sources?.split(',').map(s => {
                                                                const isNA = s.includes('NOT_AVAILABLE');
                                                                const label = s.replace('_NOT_AVAILABLE', '').replace('POLYMARKET', 'POLY').replace('KALSHI', 'KALSHI');
                                                                return (
                                                                    <span key={s} className={`px-1 text-[8px] border border-hairline rounded uppercase ${isNA ? 'bg-surface-soft/20 text-muted' : 'bg-surface-soft text-muted-foreground'}`} title={isNA ? `${label} market unavailable` : `${label} OI: $${bestOpp.marketLiquidity?.toLocaleString('en-US', {maximumFractionDigits: 0}) || 0}`}>
                                                                        {label}
                                                                    </span>
                                                                );
                                                            })}
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="p-4 text-right font-numeric font-bold h-16">
                                                    {showAsProbability ? getImpliedProb(bestOpp.bookmakerOdds) : bestOpp.bookmakerOdds.toFixed(2)}
                                                </td>
                                                <td className="p-4 text-right font-numeric text-muted h-16">{(bestOpp.trueProbability * 100).toFixed(1)}%</td>
                                                <td className="p-4 text-center font-numeric font-black text-primary h-16">+{bestOpp.evPercentage.toFixed(2)}%</td>
                                            </tr>
                                            {isExpanded && eventOpps.slice(1).map((opp) => (
                                                <tr key={opp.id} className="border-b border-hairline bg-surface-soft/30 text-sm h-14">
                                                    <td className="p-4 pl-10 text-muted italic flex items-center gap-2 h-14 truncate">
                                                        ↳ Alternative
                                                        {(opp.marketLiquidity === null || opp.marketLiquidity < 20000) && (
                                                            <span 
                                                                className="text-warning cursor-help" 
                                                                title={`Low liquidity ($${opp.marketLiquidity?.toLocaleString('en-US', {maximumFractionDigits: 0}) || 0}). True probability might be inaccurate.`}
                                                            >
                                                                ⚠️
                                                            </span>
                                                        )}
                                                    </td>
                                                    <td className="p-4 text-muted font-bold h-14 truncate">{opp.targetSelection}</td>
                                                    <td className="p-4 text-muted h-14">
                                                        <div className="flex flex-col justify-center h-full">
                                                            <span className="truncate">{opp.bookmaker}</span>
                                                            <div className="flex gap-1 mt-0.5">
                                                                {opp.sources?.split(',').map(s => {
                                                                    const isNA = s.includes('NOT_AVAILABLE');
                                                                    const label = s.replace('_NOT_AVAILABLE', '').replace('POLYMARKET', 'POLY').replace('KALSHI', 'KALSHI');
                                                                    return (
                                                                        <span key={s} className={`px-1 text-[8px] border border-hairline rounded uppercase ${isNA ? 'bg-surface-soft/20 text-muted' : 'bg-surface-soft text-muted-foreground'}`} title={isNA ? `${label} market unavailable` : `${label} OI: $${opp.marketLiquidity?.toLocaleString('en-US', {maximumFractionDigits: 0}) || 0}`}>
                                                                            {label}
                                                                        </span>
                                                                    );
                                                                })}
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td className="p-4 text-right font-numeric font-semibold h-14">
                                                        {showAsProbability ? getImpliedProb(opp.bookmakerOdds) : opp.bookmakerOdds.toFixed(2)}
                                                    </td>
                                                    <td className="p-4 text-right font-numeric text-muted h-14">{(opp.trueProbability * 100).toFixed(1)}%</td>
                                                    <td className="p-4 text-center font-numeric font-bold text-primary/70 h-14">+{opp.evPercentage.toFixed(2)}%</td>
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

            <section className="feature-card-dark bg-surface-soft">
                <h3 className="text-xl font-bold text-on-dark mb-10">Quick Execution</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8 stagger-children">
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
