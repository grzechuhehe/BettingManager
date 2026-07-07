import React, { useMemo, useState } from 'react';
import { useT } from '../../i18n/translations';

const formatPostedAt = (placedAt) => {
    if (!placedAt) return '-';
    const date = new Date(placedAt);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' });
};

const getStatusColor = (status) => {
    switch (status) {
        case 'WON': return 'text-green-500 bg-green-500/10 border-green-500/20';
        case 'LOST': return 'text-red-500 bg-red-500/10 border-red-500/20';
        case 'VOID': return 'text-gray-400 bg-gray-500/10 border-gray-500/20';
        case 'PENDING': return 'text-blue-400 bg-blue-500/10 border-blue-500/20';
        default: return 'text-gray-400 bg-gray-500/10 border-gray-500/20';
    }
};

const isGenericBuilderLabel = (selection = '') => {
    const normalized = selection.trim().toLowerCase();
    return normalized === 'bet builder' || normalized === 'betbuilder';
};

const hasExpandableLegs = (pick) => pick.legs && pick.legs.length > 0;

const isBetBuilderPick = (pick) => {
    const selection = (pick.selection || '').toLowerCase();
    return selection.includes('bet builder') || selection.includes('betbuilder');
};

const formatPickTitle = (pick) => {
    if (pick.eventName) return pick.eventName;
    if (hasExpandableLegs(pick)) return 'Parlay';
    return 'Unknown';
};

const formatPickSelection = (pick) => {
    if (hasExpandableLegs(pick)) {
        return pick.legs.map((leg) => leg.selection).filter(Boolean).join(' · ');
    }
    const selection = pick.selection || '';
    if (isGenericBuilderLabel(selection)) return 'Expand to view conditions';
    if (selection.toLowerCase().startsWith('betbuilder:')) {
        return selection.replace(/^betbuilder:\s*/i, '');
    }
    return selection || '-';
};

const SORTABLE_COLUMNS = [
    { key: 'eventName', label: 'Bet' },
    { key: 'selection', label: 'Selection' },
    { key: 'odds', label: 'Odds' },
    { key: 'stake', label: 'Stake' },
    { key: 'bookmaker', label: 'Bookie' },
    { key: 'placedAt', label: 'Posted' },
    { key: 'status', label: 'Status' },
];

const getSortValue = (pick, key, displayMode) => {
    switch (key) {
        case 'eventName': return formatPickTitle(pick).toLowerCase();
        case 'selection': return formatPickSelection(pick).toLowerCase();
        case 'odds': return pick.odds ?? 0;
        case 'stake':
            if (displayMode === 'currency') return pick.stake ?? (pick.units ?? 1) * 10;
            return pick.units ?? 1;
        case 'bookmaker': return (pick.bookmaker || '').toLowerCase();
        case 'placedAt': return pick.placedAt ? new Date(pick.placedAt).getTime() : 0;
        case 'status': return (pick.status || '').toLowerCase();
        default: return '';
    }
};

export default function PicksDataGrid({ picks, displayMode = 'units' }) {
    const translate = useT();
    const [expandedPicks, setExpandedPicks] = useState({});
    const [selectionFilter, setSelectionFilter] = useState('');
    const [sortKey, setSortKey] = useState('placedAt');
    const [sortDir, setSortDir] = useState('desc');

    const togglePick = (pickId) => {
        setExpandedPicks(prev => ({
            ...prev,
            [pickId]: !prev[pickId]
        }));
    };

    const selectionOptions = useMemo(() => {
        const values = new Set();
        picks.forEach((pick) => {
            const label = formatPickSelection(pick);
            if (label && label !== '-') values.add(label);
        });
        return [...values].sort((a, b) => a.localeCompare(b, 'en'));
    }, [picks]);

    const filteredAndSortedPicks = useMemo(() => {
        let result = [...picks];
        if (selectionFilter) {
            result = result.filter((pick) => formatPickSelection(pick) === selectionFilter);
        }
        result.sort((a, b) => {
            const aVal = getSortValue(a, sortKey, displayMode);
            const bVal = getSortValue(b, sortKey, displayMode);
            if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
            if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
            return 0;
        });
        return result;
    }, [picks, selectionFilter, sortKey, sortDir, displayMode]);

    if (!picks || picks.length === 0) {
        return (
            <div className="p-10 text-center text-muted italic">
                No AI-extracted picks found for this profile yet.
            </div>
        );
    }

    const formatStake = (pick) => {
        if (displayMode === 'currency') {
            if (pick.stake != null) {
                return `${pick.stake.toFixed(2)} PLN`;
            }
            const units = pick.units != null ? pick.units : 1;
            return `${(units * 10).toFixed(2)} PLN`;
        }
        const units = pick.units != null ? pick.units : 1;
        return `${units}u`;
    };

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap gap-4 items-end">
                <div>
                    <label className="block text-[10px] font-black text-muted uppercase tracking-widest mb-2">
                        {translate('picks.filter.selection')}
                    </label>
                    <select
                        value={selectionFilter}
                        onChange={(e) => setSelectionFilter(e.target.value)}
                        className="input-field min-w-[200px]"
                    >
                        <option value="">{translate('picks.all')}</option>
                        {selectionOptions.map((opt) => (
                            <option key={opt} value={opt}>{opt}</option>
                        ))}
                    </select>
                </div>
                <div>
                    <label className="block text-[10px] font-black text-muted uppercase tracking-widest mb-2">
                        {translate('picks.sort')}
                    </label>
                    <div className="flex gap-2">
                        <select
                            value={sortKey}
                            onChange={(e) => setSortKey(e.target.value)}
                            className="input-field min-w-[140px]"
                        >
                            {SORTABLE_COLUMNS.map((col) => (
                                <option key={col.key} value={col.key}>{col.label}</option>
                            ))}
                        </select>
                        <select
                            value={sortDir}
                            onChange={(e) => setSortDir(e.target.value)}
                            className="input-field min-w-[120px]"
                        >
                            <option value="asc">{translate('picks.sort.asc')}</option>
                            <option value="desc">{translate('picks.sort.desc')}</option>
                        </select>
                    </div>
                </div>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse table-fixed min-w-[800px]">
                    <thead>
                        <tr className="bg-surface-soft border-b border-hairline">
                            <th className="w-[28%] p-4 text-[10px] font-black text-muted uppercase tracking-widest">Bet</th>
                            <th className="w-[23%] p-4 text-[10px] font-black text-muted uppercase tracking-widest">Selection</th>
                            <th className="w-[8%] p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">Odds</th>
                            <th className="w-[12%] p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">{displayMode === 'units' ? 'Units' : 'Stake'}</th>
                            <th className="w-[10%] p-4 text-[10px] font-black text-muted uppercase tracking-widest">Bookie</th>
                            <th className="w-[10%] p-4 text-[10px] font-black text-muted uppercase tracking-widest">Posted</th>
                            <th className="w-[5%] p-4 text-[10px] font-black text-muted uppercase tracking-widest text-center">Link</th>
                            <th className="w-[10%] p-4 text-[10px] font-black text-muted uppercase tracking-widest text-right">Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredAndSortedPicks.map((pick) => {
                            const expandable = hasExpandableLegs(pick);
                            const isExpanded = expandedPicks[pick.id];
                            const builder = isBetBuilderPick(pick);

                            return (
                                <React.Fragment key={pick.id}>
                                    <tr
                                        className={`border-b border-hairline transition-colors ${expandable ? 'cursor-pointer hover:bg-surface-soft' : ''}`}
                                        onClick={() => expandable && togglePick(pick.id)}
                                    >
                                        <td className="px-4 py-4 font-bold text-on-dark h-16 align-middle">
                                            <div className="flex items-center gap-3">
                                                {expandable ? (
                                                    <span className={`text-[10px] text-muted transition-transform duration-200 ${isExpanded ? 'rotate-90' : ''}`}>▶</span>
                                                ) : (
                                                    <span className="w-2.5"></span>
                                                )}
                                                <span className="truncate flex-1" title={formatPickTitle(pick)}>
                                                    {formatPickTitle(pick)}
                                                </span>
                                                {builder && (
                                                    <span className="text-[8px] font-black uppercase tracking-widest text-primary border border-primary/30 px-1.5 py-0.5 rounded shrink-0">
                                                        Builder
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                        <td className="px-4 py-4 text-muted font-bold text-primary h-16 truncate align-middle" title={formatPickSelection(pick)}>
                                            {formatPickSelection(pick)}
                                        </td>
                                        <td className="px-4 py-4 text-right font-numeric font-bold h-16 text-on-dark align-middle">
                                            {pick.odds ? pick.odds.toFixed(2) : '-'}
                                        </td>
                                        <td className="px-4 py-4 text-right font-numeric text-muted h-16 align-middle">
                                            {formatStake(pick)}
                                        </td>
                                        <td className="px-4 py-4 text-muted h-16 truncate align-middle">
                                            {pick.bookmaker || 'Unknown'}
                                        </td>
                                        <td className="px-4 py-4 text-muted text-sm h-16 align-middle whitespace-nowrap" title={pick.placedAt || ''}>
                                            {formatPostedAt(pick.placedAt)}
                                        </td>
                                        <td className="px-4 py-4 text-center h-16 align-middle">
                                            {pick.sourcePostId ? (
                                                <a
                                                    href={`https://x.com/i/status/${pick.sourcePostId}`}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="text-muted hover:text-primary transition-colors p-1 inline-flex items-center justify-center"
                                                    onClick={(e) => e.stopPropagation()}
                                                    title="View original post"
                                                >
                                                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                                                        <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                                                    </svg>
                                                </a>
                                            ) : '-'}
                                        </td>
                                        <td className="px-4 py-4 text-right h-16 align-middle">
                                            <div className="flex flex-col items-end gap-1">
                                                <span className={`px-2.5 py-1 text-[10px] font-bold border rounded uppercase ${getStatusColor(pick.status)}`}>
                                                    {pick.status || 'PENDING'}
                                                </span>
                                                {pick.retroactiveAtImport && (pick.status === 'WON' || pick.status === 'LOST' || pick.status === 'VOID') && (
                                                    <span
                                                        className="text-[8px] text-muted font-bold uppercase tracking-tighter cursor-help"
                                                        title="This bet was already settled on the slip screenshot when Gemini extracted it — excluded from pre-match statistics."
                                                    >
                                                        ⚠️ Retroactive
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                    </tr>

                                    {isExpanded && expandable && pick.legs.map((leg, index) => (
                                        <tr key={`${pick.id}-leg-${index}`} className="border-b border-hairline bg-surface-soft/30 text-sm h-14">
                                            <td className="p-4 pl-10 text-muted italic h-14 truncate align-middle" title={leg.eventName || 'Unknown Event'}>
                                                ↳ {leg.eventName || 'Unknown Event'}
                                            </td>
                                            <td className="p-4 text-muted font-bold h-14 truncate align-middle" title={leg.selection || '-'}>
                                                {leg.selection || '-'}
                                            </td>
                                            <td className="p-4 text-right font-numeric text-muted h-14 align-middle">
                                                {leg.odds ? leg.odds.toFixed(2) : '-'}
                                            </td>
                                            <td colSpan="5" className="p-4 text-right h-14 align-middle">
                                                <span className={`px-2.5 py-1 text-[10px] font-bold border rounded uppercase ${getStatusColor(leg.status || 'PENDING')}`}>
                                                    {leg.status || 'PENDING'}
                                                </span>
                                            </td>
                                        </tr>
                                    ))}

                                    {isExpanded && pick.imageProofPath && (
                                        <tr className="border-b border-hairline bg-surface-soft/10 text-sm">
                                            <td colSpan="8" className="p-4 pl-10 text-muted">
                                                <div className="flex flex-col gap-2">
                                                    <span className="text-[10px] uppercase tracking-wider font-bold">Proof Image</span>
                                                    <img
                                                        src={`https://localhost:8443${pick.imageProofPath}`}
                                                        alt="Bet slip proof"
                                                        className="max-w-md rounded-lg border border-hairline"
                                                        onError={(e) => { e.target.style.display = 'none'; }}
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
        </div>
    );
}
