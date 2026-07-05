import React from 'react';

function Stat({ label, value }) {
    return (
        <div className="bg-surface-soft border border-hairline rounded-lg p-5 text-center">
            <p className="table-header mb-2">{label}</p>
            <p className="text-2xl font-bold text-on-dark font-numeric">{value}</p>
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
    const trackedSinceLabel = formatDate(preview?.trackedSince);

    return (
        <div className="space-y-6">
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
                <div className="flex items-center gap-5 min-w-0">
                    <div className="w-16 h-16 shrink-0 rounded-full bg-primary flex items-center justify-center text-2xl font-bold text-on-primary">
                        {loading && !preview ? (
                            <div className="w-6 h-6 border-2 border-on-primary border-t-transparent rounded-full animate-spin" />
                        ) : (
                            initial
                        )}
                    </div>
                    <div className="min-w-0">
                        <h2 className="display-sm truncate">@{displayName}</h2>
                        {loading && !preview ? (
                            <p className="body-sm text-muted mt-1">Loading profile...</p>
                        ) : isTracked ? (
                            <div className="mt-1 space-y-0.5">
                                <p className="body-sm text-body-strong">Tracked</p>
                                {trackedSinceLabel && (
                                    <p className="body-sm text-muted">Tracked since {trackedSinceLabel}</p>
                                )}
                                {preview?.lastXCheckAt && (
                                    <p className="caption text-muted">
                                        Last sync {new Date(preview.lastXCheckAt).toLocaleString('pl-PL')}
                                    </p>
                                )}
                            </div>
                        ) : (
                            <p className="body-sm text-muted mt-1">Not tracked yet</p>
                        )}
                        {loadError && (
                            <p className="caption text-warning mt-2">{loadError}</p>
                        )}
                    </div>
                </div>

                <div className="flex flex-wrap gap-3 shrink-0">
                    {isTracked ? (
                        <>
                            <button type="button" onClick={onSync} disabled={actionLoading || loading} className="button-secondary disabled:opacity-50">
                                {actionLoading ? 'Processing...' : 'Sync now'}
                            </button>
                            <button type="button" onClick={onExplore} disabled={loading} className="button-primary disabled:opacity-50">
                                Explore picks
                            </button>
                        </>
                    ) : (
                        <button type="button" onClick={onTrack} disabled={actionLoading || loading} className="button-primary disabled:opacity-50">
                            {actionLoading ? 'Verifying...' : 'Start tracking'}
                        </button>
                    )}
                </div>
            </div>

            {preview?.totalBets != null && isTracked && (
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                    <Stat label="Total bets" value={preview.totalBets} />
                    <Stat label="Win rate" value={`${preview.winRate?.toFixed?.(1) ?? 0}%`} />
                    <Stat label="Yield" value={`${preview.yield?.toFixed?.(1) ?? 0}%`} />
                    <Stat label="P/L" value={preview.totalProfitLoss?.toFixed?.(2) ?? '0.00'} />
                </div>
            )}
        </div>
    );
}
