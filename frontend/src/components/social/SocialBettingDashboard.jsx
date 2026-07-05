import { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { getProfilePreview, getTrackedProfiles, trackNewProfile, triggerManualScan } from '../../api';
import { useAuth } from '../../context/AuthContext';
import ProfilePreviewCard from './ProfilePreviewCard';

function buildPreviewFromTracked(username, trackedList) {
    const query = username.toLowerCase();
    const found = trackedList.find((p) => {
        const x = p?.xUsername?.toLowerCase();
        const u = p?.username?.toLowerCase();
        return x === query || u === query;
    });
    if (!found) {
        return {
            xUsername: username,
            xProfileUrl: `https://x.com/${username}`,
            tracked: false,
        };
    }
    return {
        xUsername: found.xUsername,
        xProfileUrl: found.xProfileUrl || `https://x.com/${found.xUsername}`,
        tracked: true,
        trackedSince: found.trackedSince,
        lastXCheckAt: found.lastXCheckAt,
    };
}

export default function SocialBettingDashboard() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { user: loggedInUser } = useAuth();

    const rawQuery = searchParams.get('q') || '';
    const searchQuery = rawQuery.replace(/^@/, '').trim();

    const [preview, setPreview] = useState(null);
    const [previewLoading, setPreviewLoading] = useState(false);
    const [previewError, setPreviewError] = useState(null);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);

    const loadPreview = useCallback(async (username, signal) => {
        if (!username) {
            setPreview(null);
            setPreviewError(null);
            return;
        }

        setPreviewLoading(true);
        setPreviewError(null);

        try {
            const res = await getProfilePreview(username, { signal });
            if (signal?.aborted) {
                return;
            }
            setPreview(res.data);
        } catch (previewErr) {
            if (previewErr.code === 'ERR_CANCELED' || previewErr.name === 'CanceledError') {
                return;
            }
            console.warn('Preview API unavailable, falling back to tracked list', previewErr);
            try {
                const trackedRes = await getTrackedProfiles({ signal });
                if (signal?.aborted) {
                    return;
                }
                const list = Array.isArray(trackedRes.data) ? trackedRes.data : [];
                setPreview(buildPreviewFromTracked(username, list));
                if (previewErr.response?.status === 404) {
                    setPreviewError('Preview endpoint not found — rebuild backend or use fallback data.');
                }
            } catch (fallbackErr) {
                if (fallbackErr.code === 'ERR_CANCELED' || fallbackErr.name === 'CanceledError') {
                    return;
                }
                console.error('Failed to load tracked profiles', fallbackErr);
                setPreview({
                    xUsername: username,
                    xProfileUrl: `https://x.com/${username}`,
                    tracked: false,
                });
                setPreviewError('Could not load profile data. Check backend connection.');
            }
        } finally {
            if (!signal?.aborted) {
                setPreviewLoading(false);
            }
        }
    }, []);

    useEffect(() => {
        setMessage(null);
        setError(null);
    }, [searchQuery]);

    useEffect(() => {
        if (!searchQuery) {
            setPreview(null);
            setPreviewError(null);
            return;
        }
        const controller = new AbortController();
        const timer = setTimeout(() => loadPreview(searchQuery, controller.signal), 200);
        return () => {
            clearTimeout(timer);
            controller.abort();
        };
    }, [searchQuery, loadPreview]);

    const handleTrackNew = async () => {
        if (!searchQuery) return;
        setLoading(true);
        setMessage(null);
        setError(null);
        try {
            const res = await trackNewProfile(searchQuery);
            setMessage(typeof res.data === 'string' ? res.data : 'Profile added to tracking.');
            await loadPreview(searchQuery);
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

    const handleForceScan = async () => {
        const xUsername = preview?.xUsername || searchQuery;
        if (!xUsername) return;
        setLoading(true);
        setMessage(null);
        setError(null);
        try {
            const res = await triggerManualScan(xUsername);
            setMessage(typeof res.data === 'string' ? res.data : 'Scan completed.');
            await loadPreview(searchQuery);
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

    const handleExplore = () => {
        const xUsername = preview?.xUsername || searchQuery;
        navigate(`/profile/${xUsername}`);
    };

    return (
        <div className="max-w-7xl mx-auto space-y-8">
            <header className="pb-6 border-b border-hairline">
                <h1 className="display-md text-on-dark">Social Betting Radar</h1>
                <p className="text-body mt-2 max-w-2xl">
                    Search for an X profile in the top navigation bar to analyze their public picks and performance.
                </p>
            </header>

            {message && (
                <div className="p-4 bg-primary/10 text-primary border border-primary/20 rounded-lg flex items-center gap-3">
                    <span className="text-lg">✓</span>
                    <span className="text-sm font-medium">{message}</span>
                </div>
            )}
            {error && (
                <div className="p-4 bg-accent-rose/10 text-accent-rose border border-accent-rose/20 rounded-lg flex items-center gap-3">
                    <span className="text-lg">⚠</span>
                    <span className="text-sm font-medium">{error}</span>
                </div>
            )}

            {searchQuery ? (
                <section className="bg-surface-card border border-hairline rounded-lg p-6 sm:p-8">
                    <ProfilePreviewCard
                        username={searchQuery}
                        preview={preview}
                        loading={previewLoading}
                        loadError={previewError}
                        actionLoading={loading}
                        onSync={handleForceScan}
                        onExplore={handleExplore}
                        onTrack={handleTrackNew}
                    />
                </section>
            ) : (
                <section className="bg-surface-card border border-hairline border-dashed rounded-lg py-20 text-center">
                    <div className="text-5xl mb-4 opacity-20">🔍</div>
                    <h3 className="text-lg font-semibold text-on-dark">Ready to track a new guru?</h3>
                    <p className="text-muted mt-2 text-sm">
                        Enter an X username in the search bar above
                        {loggedInUser && (
                            <>
                                , e.g. <span className="text-body font-mono">{loggedInUser}</span>
                            </>
                        )}
                    </p>
                </section>
            )}
        </div>
    );
}
