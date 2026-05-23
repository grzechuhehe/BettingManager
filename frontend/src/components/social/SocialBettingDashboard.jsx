import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getTrackedProfiles, trackNewProfile, triggerManualScan, getTrackedProfilePicks } from '../../api';
import PicksDataGrid from './PicksDataGrid';
import { Pagination, Box, CircularProgress, Typography, Button } from '@mui/material';

export default function SocialBettingDashboard() {
    const [profiles, setProfiles] = useState([]);
    const [selectedUsername, setSelectedUsername] = useState(null);
    const [searchParams] = useSearchParams();
    
    // Picks state
    const [picks, setPicks] = useState([]);
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(0);
    const [picksLoading, setPicksLoading] = useState(false);

    // Normalizacja nicka: usuwamy @ i białe znaki
    const rawQuery = searchParams.get('q') || '';
    const searchQuery = rawQuery.replace(/^@/, '').trim();
    
    // UI states
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);

    const loadProfiles = async () => {
        try {
            const res = await getTrackedProfiles();
            console.log("Tracked profiles received from backend:", res.data);
            setProfiles(res.data);
        } catch (err) {
            console.error("Failed to fetch profiles", err);
        }
    };

    const loadPicks = async (username, currentPage) => {
        setPicksLoading(true);
        try {
            const res = await getTrackedProfilePicks(username, currentPage - 1, 10);
            // Backend returns Page<ProfilePickDTO> which has 'content' and 'totalPages'
            setPicks(res.data.content || []);
            setTotalPages(res.data.totalPages || 0);
        } catch (err) {
            console.error("Failed to load picks", err);
            setError('Failed to load picks for this profile.');
        } finally {
            setPicksLoading(false);
        }
    };

    useEffect(() => {
        loadProfiles();
    }, []);

    useEffect(() => {
        if (selectedUsername) {
            loadPicks(selectedUsername, page);
        }
    }, [selectedUsername, page]);

    // Kasujemy błędy/komunikaty, gdy użytkownik wpisuje nowe zapytanie w pasku URL
    useEffect(() => {
        setMessage(null);
        setError(null);
        setSelectedUsername(null);
        setPage(1);
    }, [searchQuery]);

    const handleTrackNew = async () => {
        if (!searchQuery) return;
        setLoading(true);
        setMessage(null);
        setError(null);
        try {
            const res = await trackNewProfile(searchQuery);
            setMessage(res.data);
            await loadProfiles(); 
        } catch (err) {
            const data = err.response?.data;
            if (typeof data === 'object' && data !== null) {
                setError(data.message || data.error || 'An error occurred while tracking the profile.');
            } else {
                setError(data || 'An error occurred while tracking the profile.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleForceScan = async (xUsername) => {
        setLoading(true);
        setMessage(null);
        setError(null);
        try {
            const res = await triggerManualScan(xUsername);
            setMessage(res.data);
            await loadProfiles();
        } catch (err) {
            const data = err.response?.data;
            if (typeof data === 'object' && data !== null) {
                setError(data.message || data.error || 'Failed to trigger scan.');
            } else {
                setError(data || 'Failed to trigger scan.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handlePageChange = (event, value) => {
        setPage(value);
    };

    // Szukamy w bazie używając znormalizowanego nicka
    const foundProfile = profiles.find(p => 
        p && p.xUsername && searchQuery && 
        p.xUsername.toLowerCase() === searchQuery.toLowerCase()
    );

    return (
        <div className="max-w-4xl mx-auto p-4 sm:p-6 lg:p-8">
            <div className="mb-8">
                <h1 className="text-3xl font-extrabold text-white">Social Betting Radar</h1>
                <p className="mt-2 text-gray-400">Search for an X profile in the top navigation bar to analyze their bets.</p>
            </div>

            {selectedUsername ? (
                <div className="bg-[#111111] border border-white/5 p-8 rounded-2xl shadow-2xl mb-6">
                    <div className="flex justify-between items-center mb-6">
                        <Typography variant="h5" sx={{ color: 'white', fontWeight: 'bold' }}>
                            Picks by @{selectedUsername}
                        </Typography>
                        <Button 
                            onClick={() => setSelectedUsername(null)} 
                            sx={{ color: 'rgba(255, 255, 255, 0.6)', '&:hover': { color: 'white' } }}
                        >
                            &larr; Back to Search
                        </Button>
                    </div>

                    {picksLoading ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                            <CircularProgress />
                        </Box>
                    ) : (
                        <>
                            <PicksDataGrid picks={picks} />
                            {totalPages > 1 && (
                                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                                    <Pagination 
                                        count={totalPages} 
                                        page={page} 
                                        onChange={handlePageChange} 
                                        color="primary" 
                                        sx={{ 
                                            '& .MuiPaginationItem-root': { color: 'white' },
                                            '& .Mui-selected': { backgroundColor: 'rgba(25, 118, 210, 0.5) !important' }
                                        }}
                                    />
                                </Box>
                            )}
                        </>
                    )}
                </div>
            ) : (
                <div className="bg-[#111111] border border-white/5 p-8 rounded-2xl shadow-2xl mb-6">
                    {/* Messages */}
                    {message && <div className="mb-6 p-4 bg-green-500/10 text-green-400 border border-green-500/20 rounded-xl flex items-center gap-3">
                        <span className="text-xl">✓</span> {message}
                    </div>}
                    {error && <div className="mb-6 p-4 bg-red-500/10 text-red-400 border border-red-500/20 rounded-xl flex items-center gap-3">
                        <span className="text-xl">⚠️</span> {error}
                    </div>}

                    {/* Dynamic Results Area */}
                    {searchQuery.length > 0 ? (
                        <div className="p-8 border border-white/10 rounded-2xl bg-white/[0.03] flex items-center justify-between transition-all duration-300">
                            <div className="flex items-center gap-6">
                                <div className="w-16 h-16 bg-gradient-to-br from-blue-600 to-indigo-700 rounded-full flex items-center justify-center text-2xl font-black text-white shadow-lg shadow-blue-900/20">
                                    {searchQuery[0]?.toUpperCase()}
                                </div>
                                <div>
                                    <h3 className="text-2xl font-bold text-white tracking-tight">@{searchQuery}</h3>
                                    {foundProfile ? (
                                        <p className="text-sm text-green-500 mt-1 flex items-center gap-2 font-medium">
                                            <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
                                            Currently Tracked &bull; Last update: {foundProfile.lastXCheckAt ? new Date(foundProfile.lastXCheckAt).toLocaleString() : 'Ready to scan'}
                                        </p>
                                    ) : (
                                        <p className="text-sm text-gray-500 mt-1 flex items-center gap-2 font-medium">
                                            <span className="w-2 h-2 bg-gray-600 rounded-full"></span>
                                            Not Tracked Yet
                                        </p>
                                    )}
                                </div>
                            </div>
                            
                            <div className="flex gap-4">
                                {foundProfile ? (
                                    <>
                                        <button 
                                            onClick={() => handleForceScan(foundProfile.xUsername)}
                                            disabled={loading}
                                            className="px-6 py-2.5 bg-white/10 text-white hover:bg-white/20 rounded-xl font-semibold transition-colors disabled:opacity-50"
                                        >
                                            {loading ? 'Processing...' : 'Sync Now'}
                                        </button>
                                        <button 
                                            onClick={() => setSelectedUsername(foundProfile.xUsername)}
                                            className="px-6 py-2.5 bg-blue-600 text-white hover:bg-blue-700 rounded-xl font-semibold shadow-lg shadow-blue-500/20 transition-all active:scale-95"
                                        >
                                            Explore Picks
                                        </button>
                                    </>
                                ) : (
                                    <button 
                                        onClick={handleTrackNew}
                                        disabled={loading}
                                        className="px-8 py-2.5 bg-green-600 text-white hover:bg-green-700 rounded-xl font-semibold shadow-lg shadow-green-500/20 transition-all active:scale-95 disabled:opacity-50"
                                    >
                                        {loading ? 'Verifying Profile...' : 'Start Tracking'}
                                    </button>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="text-center py-16">
                            <div className="text-5xl mb-4 opacity-20 text-white">🔍</div>
                            <h3 className="text-xl font-medium text-white/40 font-semibold">Ready to track a new guru?</h3>
                            <p className="text-gray-500 mt-2">Enter an X username in the top search bar to begin analysis.</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
