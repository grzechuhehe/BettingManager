import React, { useState, useEffect } from 'react';
import { getLiveOdds } from '../api';

const FOOTBALL_LEAGUES = [
    { key: 'soccer_poland_ekstraklasa', title: 'Ekstraklasa' },
    { key: 'soccer_uefa_champs_league', title: 'Champions League' },
    { key: 'soccer_epl', title: 'Premier League' },
    { key: 'soccer_germany_bundesliga', title: 'Bundesliga' },
    { key: 'soccer_spain_la_liga', title: 'La Liga' },
    { key: 'soccer_italy_serie_a', title: 'Serie A' },
    { key: 'soccer_france_ligue_one', title: 'Ligue 1' }
];

const LiveMarkets = () => {
    const [selectedLeague, setSelectedLeague] = useState(FOOTBALL_LEAGUES[0].key);
    const [odds, setOdds] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchOdds = async () => {
            setLoading(true);
            setError(null);
            try {
                const response = await getLiveOdds(selectedLeague);
                setOdds(response.data);
            } catch (err) {
                setError('Failed to synchronize live market data. Verify API configuration.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchOdds();
    }, [selectedLeague]);

    return (
        <div className="space-y-10">
            <header className="flex justify-between items-center mb-10 pb-8 border-b border-hairline">
                <div>
                    <h1 className="display-sm flex items-center gap-4">
                        <span className="text-primary">●</span> Live Markets
                    </h1>
                    <p className="text-body mt-2">Real-time market synchronization with global sportsbooks.</p>
                </div>
                <div className="text-[10px] font-black text-muted bg-surface-card px-3 py-1 rounded-full border border-hairline uppercase tracking-widest">
                    Source: The-Odds-API
                </div>
            </header>

            {/* Wybór ligi */}
            <div className="flex gap-3 mb-10 overflow-x-auto pb-4 scrollbar-hide border-b border-hairline/50">
                {FOOTBALL_LEAGUES.map((league) => (
                    <button
                        key={league.key}
                        onClick={() => setSelectedLeague(league.key)}
                        className={`px-5 py-2.5 rounded-md font-bold transition-all duration-200 whitespace-nowrap text-[10px] uppercase tracking-widest border ${
                            selectedLeague === league.key 
                            ? 'bg-primary border-primary text-canvas' 
                            : 'bg-surface-card border-hairline text-muted hover:text-on-dark hover:border-hairline-strong'
                        }`}
                    >
                        {league.title}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className="flex flex-col items-center justify-center py-40 gap-6">
                    <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                    <p className="text-muted font-bold uppercase tracking-[0.2em] animate-pulse">Scanning Markets...</p>
                </div>
            ) : error ? (
                <div className="bg-rose-500/10 border border-rose-500/50 text-rose-500 p-8 rounded-lg flex items-center gap-6">
                    <span className="text-3xl">⚠️</span>
                    <div>
                        <p className="font-bold uppercase tracking-wider">Sync Error</p>
                        <p className="text-sm opacity-80 mt-1">{error}</p>
                    </div>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    {odds.map((match) => (
                        <div key={match.id} className="bg-surface-card p-8 rounded-lg border border-hairline hover:border-primary/50 transition-all duration-300 shadow-sm">
                            <div className="flex justify-between items-center mb-8">
                                <span className="text-muted text-[10px] font-bold uppercase tracking-widest bg-surface-elevated px-2 py-1 rounded border border-hairline">
                                    {new Date(match.commence_time).toLocaleString('en-US', {
                                        day: '2-digit',
                                        month: 'short',
                                        hour: '2-digit',
                                        minute: '2-digit',
                                        hour12: false
                                    })}
                                </span>
                                <div className="flex items-center gap-2">
                                    <span className="text-[10px] font-bold text-primary uppercase tracking-widest">Open</span>
                                </div>
                            </div>
                            
                            <div className="flex flex-col gap-6 mb-10">
                                <div className="flex justify-between items-center">
                                    <div className="flex flex-col">
                                        <span className="text-lg font-bold text-on-dark truncate max-w-[150px]">{match.home_team}</span>
                                        <span className="text-[10px] font-bold text-muted uppercase tracking-tighter mt-1">Home</span>
                                    </div>
                                    <span className="text-hairline-strong text-[10px] font-black px-4">VS</span>
                                    <div className="flex flex-col items-end">
                                        <span className="text-lg font-bold text-on-dark text-right truncate max-w-[150px]">{match.away_team}</span>
                                        <span className="text-[10px] font-bold text-muted uppercase tracking-tighter mt-1">Away</span>
                                    </div>
                                </div>
                            </div>

                            {/* Wyświetlanie kursów */}
                            <div className="grid grid-cols-3 gap-4">
                                {match.bookmakers && match.bookmakers.length > 0 ? (
                                    match.bookmakers[0].markets[0].outcomes.map((outcome) => (
                                        <button 
                                            key={outcome.name} 
                                            className="bg-surface-elevated/50 hover:bg-primary group p-4 rounded border border-hairline hover:border-primary transition-all duration-200 min-h-[64px]"
                                        >
                                            <div className="text-[10px] text-muted group-hover:text-canvas uppercase font-black mb-2 truncate tracking-widest">
                                                {outcome.name}
                                            </div>
                                            <div className="text-xl font-bold text-primary group-hover:text-canvas font-numeric">
                                                {outcome.price.toFixed(2)}
                                            </div>
                                        </button>
                                    ))
                                ) : (
                                    <div className="col-span-3 py-6 text-center text-muted text-[10px] font-bold uppercase tracking-widest bg-surface-elevated/30 rounded border border-dashed border-hairline">
                                        No active market metrics
                                    </div>
                                )}
                            </div>
                            
                            <div className="mt-8 pt-6 border-t border-hairline flex justify-between items-center">
                                <div className="flex items-center gap-2">
                                    <span className="text-[10px] text-muted uppercase tracking-widest font-bold">Best Execution:</span>
                                    <span className="text-[10px] font-black text-on-dark uppercase tracking-widest">{match.bookmakers?.[0]?.title || 'N/A'}</span>
                                </div>
                                <button className="text-[10px] font-black uppercase tracking-widest text-primary hover:text-on-dark transition-colors">
                                    Market Analysis →
                                </button>
                            </div>
                        </div>
                    ))}
                    
                    {odds.length === 0 && !loading && (
                        <div className="col-span-full flex flex-col items-center justify-center py-32 surface-card border-dashed">
                            <span className="text-4xl mb-6 opacity-30">📅</span>
                            <p className="text-on-dark font-bold uppercase tracking-widest text-lg">No sessions scheduled.</p>
                            <p className="text-muted text-xs mt-2 uppercase tracking-widest font-bold">Check alternative market endpoints or return later.</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default LiveMarkets;
