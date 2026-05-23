import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getTrackedProfilePicks, getTrackedProfileStats } from '../../api';
import PicksDataGrid from './PicksDataGrid';

export default function TrackedProfileView() {
    const { username } = useParams();
    const navigate = useNavigate();
    
    const [stats, setStats] = useState(null);
    const [picks, setPicks] = useState([]);
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [displayMode, setDisplayMode] = useState('units'); // 'units' or 'currency'

    useEffect(() => {
        const fetchProfileData = async () => {
            setLoading(true);
            setError(null);
            try {
                const [statsRes, picksRes] = await Promise.all([
                    getTrackedProfileStats(username),
                    getTrackedProfilePicks(username, page - 1, 10)
                ]);
                setStats(statsRes.data);
                setPicks(picksRes.data.content || []);
                setTotalPages(picksRes.data.totalPages || 0);
            } catch (err) {
                console.error("Failed to fetch profile data:", err);
                setError("Failed to load profile data.");
            } finally {
                setLoading(false);
            }
        };

        if (username) {
            fetchProfileData();
        }
    }, [username, page]);

    if (loading && !stats) {
        return (
            <div className="flex flex-col items-center justify-center py-40 gap-6">
                <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-muted font-bold uppercase tracking-[0.2em] animate-pulse">Loading Profile...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="max-w-7xl mx-auto p-6 text-center">
                <div className="p-4 bg-red-500/10 text-red-400 border border-red-500/20 rounded-xl inline-block">
                    {error}
                </div>
                <div className="mt-4">
                    <button onClick={() => navigate('/social')} className="text-blue-400 hover:underline">
                        &larr; Back to Search
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto space-y-12">
            <header className="pb-8 border-b border-hairline flex justify-between items-end">
                <div>
                    <h2 className="display-md">@{username}</h2>
                    <p className="text-body mt-2">Public Betting Profile & Statistics</p>
                </div>
                <div className="flex gap-4">
                    <div className="flex bg-surface-card border border-hairline rounded-xl p-1">
                        <button 
                            onClick={() => setDisplayMode('units')}
                            className={`px-4 py-1.5 text-xs font-bold rounded-lg uppercase tracking-widest transition-colors ${displayMode === 'units' ? 'bg-primary text-canvas' : 'text-muted hover:text-on-dark'}`}
                        >
                            Units
                        </button>
                        <button 
                            onClick={() => setDisplayMode('currency')}
                            className={`px-4 py-1.5 text-xs font-bold rounded-lg uppercase tracking-widest transition-colors ${displayMode === 'currency' ? 'bg-primary text-canvas' : 'text-muted hover:text-on-dark'}`}
                        >
                            PLN
                        </button>
                    </div>
                    <button 
                        onClick={() => navigate('/social')}
                        className="px-6 py-2 bg-surface-soft hover:bg-surface-elevated text-on-dark rounded-xl transition-colors border border-hairline"
                    >
                        &larr; Back to Search
                    </button>
                </div>
            </header>

            {/* Top Stats Section (Like Dashboard) */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="bg-surface-card p-8 rounded-xl border border-hairline text-center">
                    <p className="text-[10px] font-black text-muted uppercase tracking-widest mb-2">Total Positions</p>
                    <p className="text-3xl font-black text-on-dark font-numeric">{stats?.totalBets || 0}</p>
                </div>
                <div className="bg-surface-card p-8 rounded-xl border border-hairline text-center">
                    <p className="text-[10px] font-black text-muted uppercase tracking-widest mb-2">Win Rate</p>
                    <p className="text-3xl font-black text-primary font-numeric">{stats?.winRate?.toFixed(1) || 0}%</p>
                </div>
                <div className="bg-surface-card p-8 rounded-xl border border-hairline text-center">
                    <p className="text-[10px] font-black text-muted uppercase tracking-widest mb-2">Total Staked {displayMode === 'units' ? '(u)' : '(PLN)'}</p>
                    <p className="text-3xl font-black text-on-dark font-numeric">
                        {displayMode === 'units' 
                            ? (stats?.totalStaked?.toFixed(2) || 0)
                            : ((stats?.totalStaked || 0) * 10).toFixed(2)
                        }
                    </p>
                </div>
                <div className="bg-surface-card p-8 rounded-xl border border-hairline text-center">
                    <p className="text-[10px] font-black text-muted uppercase tracking-widest mb-2">Net Profit {displayMode === 'units' ? '(u)' : '(PLN)'}</p>
                    <p className={`text-3xl font-black font-numeric ${(stats?.totalProfitLoss || 0) >= 0 ? 'text-primary' : 'text-rose-500'}`}>
                        {(stats?.totalProfitLoss || 0) > 0 ? '+' : ''}
                        {displayMode === 'units'
                            ? (stats?.totalProfitLoss?.toFixed(2) || '0.00')
                            : ((stats?.totalProfitLoss || 0) * 10).toFixed(2)
                        }
                    </p>
                </div>
            </div>

            {/* Bottom Picks Section */}
            <section className="bg-surface-card border border-hairline rounded-xl p-8">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xl font-bold text-on-dark">Bet History</h3>
                </div>
                
                {loading && picks.length === 0 ? (
                    <div className="flex justify-center py-12">
                        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                    </div>
                ) : (
                    <div className="w-full">
                        <PicksDataGrid picks={picks} displayMode={displayMode} />
                        
                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="flex justify-center items-center gap-4 mt-8">
                                <button 
                                    disabled={page === 1}
                                    onClick={() => setPage(p => Math.max(1, p - 1))}
                                    className="px-4 py-2 bg-surface-soft hover:bg-surface-elevated disabled:opacity-30 disabled:hover:bg-surface-soft rounded text-on-dark transition-colors border border-hairline"
                                >
                                    Previous
                                </button>
                                <span className="text-muted text-sm font-bold">
                                    Page {page} of {totalPages}
                                </span>
                                <button 
                                    disabled={page === totalPages}
                                    onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                                    className="px-4 py-2 bg-surface-soft hover:bg-surface-elevated disabled:opacity-30 disabled:hover:bg-surface-soft rounded text-on-dark transition-colors border border-hairline"
                                >
                                    Next
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </section>
        </div>
    );
}
