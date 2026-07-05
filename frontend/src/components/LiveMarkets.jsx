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
                    <h1 className="display-sm">Live Markets</h1>
                    <p className="text-body mt-2">Real-time market synchronization with global sportsbooks.</p>
                </div>
                <div className="badge-pill">Source: The-Odds-API</div>
            </header>

            <div className="flex gap-2 mb-10 overflow-x-auto pb-4 border-b border-hairline scrollbar-hide">
                {FOOTBALL_LEAGUES.map((league) => (
                    <button
                        key={league.key}
                        type="button"
                        onClick={() => setSelectedLeague(league.key)}
                        className={`shrink-0 whitespace-nowrap ${selectedLeague === league.key ? 'category-tab-active' : 'category-tab'}`}
                    >
                        {league.title}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className="flex flex-col items-center justify-center py-40 gap-6">
                    <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                    <p className="body-sm text-muted">Scanning markets...</p>
                </div>
            ) : error ? (
                <div className="flex justify-center py-32">
                    <div className="alert-error max-w-xl w-full">
                        <p className="font-semibold">Sync error</p>
                        <p className="body-sm mt-2 normal-case tracking-normal font-normal">{error}</p>
                    </div>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 stagger-children">
                    {odds.map((match) => (
                        <div key={match.id} className="feature-card-dark p-8">
                            <div className="flex justify-between items-center mb-8">
                                <span className="badge-pill">
                                    {new Date(match.commence_time).toLocaleString('en-US', {
                                        day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false
                                    })}
                                </span>
                                <span className="badge-yellow">Open</span>
                            </div>

                            <div className="flex justify-between items-center mb-10">
                                <div>
                                    <p className="title-sm truncate max-w-[150px]">{match.home_team}</p>
                                    <p className="caption text-muted mt-1">Home</p>
                                </div>
                                <span className="caption-uppercase text-muted-soft px-4">VS</span>
                                <div className="text-right">
                                    <p className="title-sm truncate max-w-[150px]">{match.away_team}</p>
                                    <p className="caption text-muted mt-1">Away</p>
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                {match.bookmakers?.[0]?.markets?.[0]?.outcomes?.map((outcome) => (
                                    <div key={outcome.name} className="bg-surface-elevated border border-hairline rounded-md p-4 min-h-[64px]">
                                        <p className="caption text-muted mb-2 truncate">{outcome.name}</p>
                                        <p className="text-xl font-bold text-primary font-numeric">{outcome.price.toFixed(2)}</p>
                                    </div>
                                )) || (
                                    <div className="col-span-3 py-6 text-center caption text-muted bg-surface-soft rounded-md border border-dashed border-hairline">
                                        No active market metrics
                                    </div>
                                )}
                            </div>

                            <div className="mt-8 pt-6 border-t border-hairline flex justify-between items-center">
                                <p className="caption text-muted">
                                    Best execution: <span className="text-on-dark">{match.bookmakers?.[0]?.title || 'N/A'}</span>
                                </p>
                                <button type="button" className="text-link caption">Market analysis</button>
                            </div>
                        </div>
                    ))}

                    {odds.length === 0 && (
                        <div className="col-span-full surface-card border-dashed text-center py-32">
                            <p className="title-md mb-2">No sessions scheduled</p>
                            <p className="body-sm text-muted">Check alternative market endpoints or return later.</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default LiveMarkets;
