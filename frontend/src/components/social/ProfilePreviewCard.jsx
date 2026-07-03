import React from 'react';

function Stat({ label, value }) {
    return (
        <div className="bg-surface-soft border border-hairline rounded-xl p-5 text-center">
            <p className="text-[10px] font-black text-muted uppercase tracking-widest mb-2">{label}</p>
            <p className="text-2xl font-black text-on-dark font-numeric">{value}</p>
        </div>
    );
}

const formatDate = (iso) => {
    if (!iso) return null;
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return null;
    return date.toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' });
};

export default function ProfilePreviewCard({
    username,
    preview,
    loading,
    loadError,
    onSync,
    onExplore,
    onTrack,
    actionLoading,
}) {
    const displayName = preview?.xUsername || username;
    const initial = displayName?.[0]?.toUpperCase() || '?';
    const isTracked = Boolean(preview?.tracked);
    const hasStats = isTracked && preview?.totalBets != null;
    const trackedSinceLabel = formatDate(preview?.trackedSince);

    return (
        <div className="space-y-6">
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
                <div className="flex items-center gap-5 min-w-0">
                    <div className="w-16 h-16 shrink-0 rounded-full bg-primary flex items-center justify-center text-2xl font-black text-canvas shadow-lg shadow-primary/10">
                        {loading && !preview ? (
                            <div className="w-6 h-6 border-2 border-canvas border-t-transparent rounded-full animate-spin" />
                        ) : (
                            initial
                        )}
                    </div>
                    <div className="min-w-0">
                        <h2 className="text-2xl sm:text-3xl font-bold text-on-dark tracking-tight truncate">
                            @{displayName}
                        </h2>
                        {loading && !preview ? (
                            <p className="text-sm text-muted mt-1">Loading profile…</p>
                        ) : isTracked ? (
                            <div className="mt-1 space-y-0.5">
                                <p className="text-sm text-primary flex items-center gap-2 font-medium">
                                    <span className="w-2 h-2 bg-primary rounded-full animate-pulse shrink-0" />
                                    Tracked
                                </p>
                                {trackedSinceLabel && (
                                    <p className="text-sm text-muted">
                                        Tracked since {trackedSinceLabel}
                                    </p>
                                )}
                                {preview?.lastXCheckAt && (
                                    <p className="text-xs text-muted">
                                        Last sync {new Date(preview.lastXCheckAt).toLocaleString('pl-PL')}
                                    </p>
                                )}
                            </div>
                        ) : (
                            <p className="text-sm text-muted mt-1">Not tracked yet</p>
                        )}
                        {loadError && (
                            <p className="text-xs text-amber-400/90 mt-2">{loadError}</p>
                        )}
                    </div>
                </div>

                <div className="flex flex-wrap gap-3 shrink-0">
                    {isTracked ? (
                        <>
                            <button
                                type="button"
                                onClick={onSync}
                                disabled={actionLoading || loading}
                                className="px-5 py-2.5 bg-surface-soft hover:bg-surface-elevated text-on-dark rounded-xl font-semibold border border-hairline transition-colors disabled:opacity-50"
                            >
                                {actionLoading ? 'Processing…' : 'Sync Now'}
                            </button>
                            <button
                                type="button"
                                onClick={onExplore}
                                disabled={loading}
                                className="px-5 py-2.5 bg-primary text-canvas hover:bg-primary-active rounded-xl font-semibold transition-all active:scale-[0.98] disabled:opacity-50"
                            >
                                Explore Picks
                            </button>
                        </>
                    ) : (
                        <button
                            type="button"
                            onClick={onTrack}
                            disabled={actionLoading || loading}
                            className="px-6 py-2.5 bg-primary text-canvas hover:bg-primary-active rounded-xl font-semibold transition-all active:scale-[0.98] disabled:opacity-50"
                        >
                            {actionLoading ? 'Verifying…' : 'Start Tracking'}
                        </button>
                    )}
                </div>
            </div>

            {isTracked && hasStats && (
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <Stat label="Positions" value={preview.totalBets ?? 0} />
                    <Stat label="Win rate" value={`${Number(preview.winRate ?? 0).toFixed(1)}%`} />
                    <Stat label="Yield" value={`${Number(preview.yield ?? 0).toFixed(2)}%`} />
                </div>
            )}

            {isTracked && !hasStats && !loading && (
                <p className="text-sm text-muted border-t border-hairline pt-4">
                    Profile is tracked. Open Explore Picks for full history and statistics.
                </p>
            )}
        </div>
    );
}
