import React, { useState } from 'react';

const getStatusColor = (status) => {
    switch (status) {
        case 'WON': return 'text-green-500 bg-green-500/10 border-green-500/20';
        case 'LOST': return 'text-red-500 bg-red-500/10 border-red-500/20';
        case 'VOID': return 'text-gray-400 bg-gray-500/10 border-gray-500/20';
        case 'PENDING': return 'text-blue-400 bg-blue-500/10 border-blue-500/20';
        default: return 'text-gray-400 bg-gray-500/10 border-gray-500/20';
    }
};

export default function PicksDataGrid({ picks, displayMode = 'units' }) {
    const [expandedPicks, setExpandedPicks] = useState({});

    const togglePick = (pickId) => {
        setExpandedPicks(prev => ({
            ...prev,
            [pickId]: !prev[pickId]
        }));
    };

    if (!picks || picks.length === 0) {
        return (
            <div className="p-10 text-center text-muted italic">
                No AI-extracted picks found for this profile yet.
            </div>
        );
    }

    const formatStake = (pick) => {
        if (displayMode === 'currency') {
            // Prefer actual stake from DB, fallback to units * 10
            if (pick.stake != null) {
                return `${pick.stake.toFixed(2)} PLN`;
            }
            const units = pick.units != null ? pick.units : 1;
            return `${(units * 10).toFixed(2)} PLN`;
        }
        
        // For units mode, use units from DB
        const units = pick.units != null ? pick.units : 1;
        return `${units}u`;
    };

    return (
        <div className="bg-surface-card border border-hairline rounded-lg overflow-hidden">
            <table className="w-full text-left border-collapse table-fixed">
                <thead>
                    <tr className="bg-surface-soft border-b border-hairline">
                        <th className="w-1/3 p-4 text-[10px] font-black text-muted uppercase tracking-widest">Bet</th>
                        <th className="w-1/6 p-4 text-[10px] font-black text-muted uppercase tracking-widest">Selection</th>
                        <th className="w-1/12 p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">Odds</th>
                        <th className="w-1/12 p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">{displayMode === 'units' ? 'Units' : 'Stake'}</th>
                        <th className="w-1/6 p-4 text-[10px] font-black text-muted uppercase tracking-widest">Bookmaker</th>
                        <th className="w-1/6 p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">Status</th>
                    </tr>
                </thead>
                <tbody>
                    {picks.map((pick) => {
                        const isParlay = pick.legs && pick.legs.length > 0;
                        const isExpanded = expandedPicks[pick.id];

                        return (
                            <React.Fragment key={pick.id}>
                                <tr 
                                    className={`border-b border-hairline transition-colors ${isParlay ? 'cursor-pointer hover:bg-surface-soft' : ''}`}
                                    onClick={() => isParlay && togglePick(pick.id)}
                                >
                                    <td className="p-4 font-bold text-on-dark h-16">
                                        <div className="flex items-center gap-3 h-full">
                                            {isParlay ? (
                                                <span className={`text-[10px] text-muted transition-transform duration-200 ${isExpanded ? 'rotate-90' : ''}`}>▶</span>
                                            ) : (
                                                <span className="w-2.5"></span> /* Spacer for alignment */
                                            )}
                                            <span className="truncate flex-1" title={pick.eventName || (isParlay ? 'Parlay' : 'Unknown')}>
                                                {pick.eventName || (isParlay ? 'Parlay' : 'Unknown')}
                                            </span>
                                            {pick.sourcePostId && (
                                                <a 
                                                    href={`https://x.com/i/status/${pick.sourcePostId}`}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="text-muted hover:text-primary transition-colors p-1"
                                                    onClick={(e) => e.stopPropagation()}
                                                    title="View original post"
                                                >
                                                    <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24">
                                                        <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                                                    </svg>
                                                </a>
                                            )}
                                        </div>
                                    </td>
                                    <td className="p-4 text-muted font-bold text-primary h-16 truncate">
                                        {pick.selection || (isParlay ? 'Multiple Selections' : '-')}
                                    </td>
                                    <td className="p-4 text-right font-numeric font-bold h-16 text-on-dark">
                                        {pick.odds ? pick.odds.toFixed(2) : '-'}
                                    </td>
                                    <td className="p-4 text-right font-numeric text-muted h-16">
                                        {formatStake(pick)}
                                    </td>
                                    <td className="p-4 text-muted h-16 truncate">
                                        {pick.bookmaker || 'Unknown'}
                                    </td>
                                    <td className="p-4 text-right h-16">
                                        <span className={`px-2.5 py-1 text-[10px] font-bold border rounded uppercase ${getStatusColor(pick.status)}`}>
                                            {pick.status || 'PENDING'}
                                        </span>
                                    </td>
                                </tr>
                                
                                {isExpanded && isParlay && pick.legs.map((leg, index) => (
                                    <tr key={`${pick.id}-leg-${index}`} className="border-b border-hairline bg-surface-soft/30 text-sm h-14">
                                        <td className="p-4 pl-10 text-muted italic flex items-center gap-2 h-14 truncate" title={leg.eventName || 'Unknown Event'}>
                                            ↳ {leg.eventName || 'Unknown Event'}
                                        </td>
                                        <td className="p-4 text-muted font-bold h-14 truncate">
                                            {leg.selection || '-'}
                                        </td>
                                        <td className="p-4 text-right font-numeric text-muted h-14">
                                            {leg.odds ? leg.odds.toFixed(2) : '-'}
                                        </td>
                                        <td colSpan="3" className="p-4 text-right text-muted italic h-14">
                                            {/* Empty space for legs as they share units/status/bookmaker with parent */}
                                        </td>
                                    </tr>
                                ))}
                                
                                {/* Image proof row if available (could be placed inside expansion, but keeping it visible for now or maybe expandable too) */}
                                {isExpanded && pick.imageProofPath && (
                                    <tr className="border-b border-hairline bg-surface-soft/10 text-sm">
                                        <td colSpan="6" className="p-4 pl-10 text-muted">
                                            <div className="flex flex-col gap-2">
                                                <span className="text-[10px] uppercase tracking-wider font-bold">Proof Image</span>
                                                <img 
                                                    src={`https://localhost:8443${pick.imageProofPath}`} 
                                                    alt="Bet slip proof" 
                                                    className="max-w-md rounded-lg border border-hairline"
                                                    onError={(e) => e.target.style.display = 'none'}
                                                />
                                            </div>
                                        </td>
                                    </tr>
                                )}
                            </React.Fragment>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
