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
                setError('Błąd podczas pobierania kursów. Sprawdź konfigurację API lub czy liga jest obecnie dostępna.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchOdds();
    }, [selectedLeague]);

    return (
        <div className="p-6 bg-slate-900 min-h-screen text-white rounded-xl shadow-2xl">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-3xl font-bold flex items-center gap-3">
                    <span className="text-4xl">⚽</span> Live Markets
                </h1>
                <div className="text-xs text-slate-400 bg-slate-800 px-3 py-1 rounded-full border border-slate-700">
                    Data source: The-Odds-API
                </div>
            </div>

            {/* Wybór ligi */}
            <div className="flex gap-2 mb-8 overflow-x-auto pb-4 scrollbar-hide">
                {FOOTBALL_LEAGUES.map((league) => (
                    <button
                        key={league.key}
                        onClick={() => setSelectedLeague(league.key)}
                        className={`px-5 py-2.5 rounded-xl font-medium transition-all duration-200 whitespace-nowrap border ${
                            selectedLeague === league.key 
                            ? 'bg-blue-600 border-blue-400 text-white shadow-lg shadow-blue-900/40 translate-y-[-2px]' 
                            : 'bg-slate-800 border-slate-700 text-slate-400 hover:bg-slate-700 hover:text-white'
                        }`}
                    >
                        {league.title}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className="flex flex-col items-center justify-center py-32 gap-4">
                    <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-b-4 border-blue-500 shadow-lg shadow-blue-500/20"></div>
                    <p className="text-slate-400 animate-pulse font-medium">Fetching live data...</p>
                </div>
            ) : error ? (
                <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-6 rounded-2xl flex items-center gap-4">
                    <span className="text-2xl">⚠️</span>
                    <div>
                        <p className="font-bold">Błąd połączenia</p>
                        <p className="text-sm opacity-80">{error}</p>
                    </div>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {odds.map((match) => (
                        <div key={match.id} className="group bg-slate-800/50 backdrop-blur-sm p-6 rounded-2xl border border-slate-700 hover:border-blue-500/50 hover:bg-slate-800 transition-all duration-300 shadow-sm hover:shadow-xl hover:shadow-blue-900/10">
                            <div className="flex justify-between items-center mb-6">
                                <span className="text-slate-400 text-xs font-mono bg-slate-900/50 px-2 py-1 rounded">
                                    {new Date(match.commence_time).toLocaleString('pl-PL', {
                                        day: '2-digit',
                                        month: 'short',
                                        hour: '2-digit',
                                        minute: '2-digit'
                                    })}
                                </span>
                                <div className="h-2 w-2 rounded-full bg-green-500 animate-pulse"></div>
                            </div>
                            
                            <div className="flex flex-col gap-4 mb-8">
                                <div className="flex justify-between items-center">
                                    <span className="text-lg font-bold truncate max-w-[150px]">{match.home_team}</span>
                                    <span className="text-slate-500 text-xs font-black px-2">VS</span>
                                    <span className="text-lg font-bold text-right truncate max-w-[150px]">{match.away_team}</span>
                                </div>
                            </div>

                            {/* Wyświetlanie kursów u najlepszego bukmachera */}
                            <div className="grid grid-cols-3 gap-3">
                                {match.bookmakers && match.bookmakers.length > 0 ? (
                                    match.bookmakers[0].markets[0].outcomes.map((outcome) => (
                                        <button 
                                            key={outcome.name} 
                                            className="bg-slate-900/50 hover:bg-blue-600 group/odds p-3 rounded-xl border border-slate-700 hover:border-blue-400 transition-all duration-200"
                                        >
                                            <div className="text-[10px] text-slate-500 group-hover/odds:text-blue-100 uppercase font-bold mb-1 truncate">
                                                {outcome.name}
                                            </div>
                                            <div className="text-xl font-mono text-blue-400 group-hover/odds:text-white">
                                                {outcome.price.toFixed(2)}
                                            </div>
                                        </button>
                                    ))
                                ) : (
                                    <div className="col-span-3 py-4 text-center text-slate-500 text-sm italic bg-slate-900/30 rounded-xl">
                                        No live markets available
                                    </div>
                                )}
                            </div>
                            
                            <div className="mt-6 pt-4 border-t border-slate-700/50 flex justify-between items-center">
                                <span className="text-[10px] text-slate-500 uppercase tracking-wider font-bold">
                                    Best Odds: <span className="text-slate-300 ml-1">{match.bookmakers?.[0]?.title || 'N/A'}</span>
                                </span>
                                <button className="text-[10px] bg-blue-600/10 hover:bg-blue-600/20 text-blue-400 px-3 py-1 rounded-lg border border-blue-500/20 transition-colors">
                                    Analysis
                                </button>
                            </div>
                        </div>
                    ))}
                    
                    {odds.length === 0 && !loading && (
                        <div className="col-span-full flex flex-col items-center justify-center py-20 bg-slate-800/20 rounded-3xl border-2 border-dashed border-slate-700">
                            <span className="text-5xl mb-4 opacity-20">📅</span>
                            <p className="text-slate-500 font-medium text-lg">Brak nadchodzących meczów dla tej ligi.</p>
                            <p className="text-slate-600 text-sm mt-1">Wróć później lub wybierz inną ligę.</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default LiveMarkets;
