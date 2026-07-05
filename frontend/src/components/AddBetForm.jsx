import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { addBet } from '../api';
import ImportBetFromImage from './ImportBetFromImage';

let nextId = 1;
const createInitialLeg = () => ({
    id: nextId++,
    sport: 'Football',
    eventName: '',
    eventDate: '',
    marketType: 'MONEYLINE_1X2',
    selection: '',
    odds: '',
    bookmaker: 'STS',
});

const marketTypes = ['MONEYLINE_1X2', 'MONEYLINE_12', 'TOTALS_OVER_UNDER', 'HANDICAP', 'ASIAN_HANDICAP', 'CORRECT_SCORE', 'PLAYER_PROPS', 'BOTH_TEAMS_TO_SCORE', 'OUTRIGHT', 'OTHER'];
const marketLabels = {
    MONEYLINE_1X2: '1X2 (Match Result)',
    MONEYLINE_12: '1/2 (No Draw)',
    TOTALS_OVER_UNDER: 'Over / Under',
    HANDICAP: 'Handicap',
    ASIAN_HANDICAP: 'Asian Handicap',
    CORRECT_SCORE: 'Correct Score',
    PLAYER_PROPS: 'Player Props',
    BOTH_TEAMS_TO_SCORE: 'Both Teams to Score',
    OUTRIGHT: 'Outright',
    OTHER: 'Other',
};
const bookmakers = ['STS', 'Fortuna', 'Betclic', 'Superbet', 'forBET', 'eTOTO', 'LVBET', 'Totalbet', 'Betfan', 'Fuksiarz', 'Betters', 'GoBet'];
const sports = ['Football', 'Basketball', 'Tennis', 'Ice Hockey', 'MMA', 'Boxing', 'F1', 'CS:GO', 'LoL', 'Valorant', 'Other'];

const FormField = ({ label, htmlFor, children, className = '' }) => (
    <div className={className}>
        <label htmlFor={htmlFor} className="field-label">{label}</label>
        {children}
    </div>
);

const SectionDivider = ({ label }) => (
    <div className="relative py-2">
        <div className="absolute inset-0 flex items-center" aria-hidden="true">
            <div className="w-full border-t border-hairline" />
        </div>
        <div className="relative flex justify-center">
            <span className="bg-surface-card px-4 caption-uppercase text-muted">{label}</span>
        </div>
    </div>
);

const LegFormField = ({ label, htmlFor, children, className = '' }) => (
    <div className={className}>
        <label htmlFor={htmlFor} className="field-label !text-[10px] !mb-2 opacity-70">{label}</label>
        {children}
    </div>
);

const LegSection = ({ step, title, children }) => (
    <div className="space-y-4">
        <div className="flex items-center gap-2">
            <span className="text-sm font-bold text-primary tabular-nums">{step}</span>
            <span className="field-label !text-[10px] !mb-0">{title}</span>
        </div>
        {children}
    </div>
);

const BetLegCard = ({ leg, index, totalLegs, onChange, onRemove }) => (
    <article className="relative overflow-hidden rounded-lg border border-hairline bg-surface-card">
        <div className="absolute inset-y-0 left-0 w-1 bg-primary" aria-hidden="true" />

        <div className="space-y-6 p-6 pl-7 md:space-y-8 md:p-8">
            <header className="flex items-start justify-between gap-4">
                <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-3">
                        <span className="rounded bg-primary px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-on-primary tabular-nums">
                            Leg {index + 1}
                        </span>
                        <h3 className="text-sm font-bold uppercase tracking-wider text-on-dark">Market parameters</h3>
                        {totalLegs > 1 && (
                            <span className="caption-uppercase text-muted">Parlay leg</span>
                        )}
                    </div>
                    <p className="text-xs text-muted">Event, bookmaker and your pick for this leg.</p>
                </div>
                {totalLegs > 1 && (
                    <button
                        type="button"
                        onClick={() => onRemove(leg.id)}
                        className="shrink-0 rounded-md border border-accent-rose/30 bg-accent-rose/10 px-3 py-1.5 text-xs font-bold uppercase tracking-wider text-accent-rose transition-colors hover:bg-accent-rose hover:text-white"
                    >
                        Remove
                    </button>
                )}
            </header>

            <LegSection step={1} title="Event">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    <LegFormField label="Event name" htmlFor={`eventName-${leg.id}`}>
                        <input
                            id={`eventName-${leg.id}`}
                            type="text"
                            name="eventName"
                            placeholder="e.g. Real Madrid vs Barcelona"
                            value={leg.eventName}
                            onChange={(e) => onChange(leg.id, e)}
                            required
                            className="input-field !h-auto py-2.5 text-sm"
                        />
                    </LegFormField>
                    <LegFormField label="Kick-off time" htmlFor={`eventDate-${leg.id}`}>
                        <input
                            id={`eventDate-${leg.id}`}
                            type="datetime-local"
                            name="eventDate"
                            value={leg.eventDate}
                            onChange={(e) => onChange(leg.id, e)}
                            required
                            className="input-field !h-auto py-2.5 text-sm [color-scheme:dark]"
                        />
                    </LegFormField>
                </div>
            </LegSection>

            <LegSection step={2} title="Source">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    <LegFormField label="Bookmaker" htmlFor={`bookmaker-${leg.id}`}>
                        <select
                            id={`bookmaker-${leg.id}`}
                            name="bookmaker"
                            value={leg.bookmaker}
                            onChange={(e) => onChange(leg.id, e)}
                            className="input-field !h-auto py-2.5 text-sm"
                        >
                            {bookmakers.map((b) => <option key={b} value={b}>{b}</option>)}
                        </select>
                    </LegFormField>
                    <LegFormField label="Sport" htmlFor={`sport-${leg.id}`}>
                        <select
                            id={`sport-${leg.id}`}
                            name="sport"
                            value={leg.sport}
                            onChange={(e) => onChange(leg.id, e)}
                            className="input-field !h-auto py-2.5 text-sm"
                        >
                            {sports.map((s) => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </LegFormField>
                </div>
            </LegSection>

            <LegSection step={3} title="Your pick">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                    <LegFormField label="Market type" htmlFor={`marketType-${leg.id}`}>
                        <select
                            id={`marketType-${leg.id}`}
                            name="marketType"
                            value={leg.marketType}
                            onChange={(e) => onChange(leg.id, e)}
                            className="input-field !h-auto py-2.5 text-sm"
                        >
                            {marketTypes.map((mt) => (
                                <option key={mt} value={mt}>{marketLabels[mt] || mt}</option>
                            ))}
                        </select>
                    </LegFormField>
                    <LegFormField label="Selection" htmlFor={`selection-${leg.id}`}>
                        <input
                            id={`selection-${leg.id}`}
                            type="text"
                            name="selection"
                            placeholder="e.g. Real Madrid to win"
                            value={leg.selection}
                            onChange={(e) => onChange(leg.id, e)}
                            required
                            className="input-field !h-auto py-2.5 text-sm"
                        />
                    </LegFormField>
                    <LegFormField label="Odds" htmlFor={`odds-${leg.id}`}>
                        <div className="relative">
                            <input
                                id={`odds-${leg.id}`}
                                type="number"
                                name="odds"
                                placeholder="1.85"
                                step="0.01"
                                min="1.01"
                                value={leg.odds}
                                onChange={(e) => onChange(leg.id, e)}
                                required
                                className="input-field !h-auto py-2.5 pr-10 text-sm font-numeric font-bold text-primary"
                            />
                            <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[9px] font-semibold uppercase tracking-wider text-muted">
                                Dec
                            </span>
                        </div>
                    </LegFormField>
                </div>
            </LegSection>
        </div>
    </article>
);

const AddBetForm = () => {
    const navigate = useNavigate();
    const [legs, setLegs] = useState([createInitialLeg()]);
    const [stake, setStake] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);

    const handleLegChange = (id, e) => {
        const { name, value } = e.target;
        setLegs(legs.map(leg =>
            leg.id === id ? { ...leg, [name]: value } : leg
        ));
    };

    const addLeg = () => {
        setLegs([...legs, createInitialLeg()]);
    };

    const removeLeg = (id) => {
        setLegs(legs.filter(leg => leg.id !== id));
    };

    const { totalOdds, potentialWinnings } = useMemo(() => {
        if (legs.length === 0 || !stake) {
            return { totalOdds: '—', potentialWinnings: '—' };
        }
        const totalOddsValue = legs.reduce((acc, leg) => acc * (parseFloat(leg.odds) || 1), 1);
        const potentialWinningsValue = totalOddsValue * parseFloat(stake);
        return {
            totalOdds: totalOddsValue.toFixed(2),
            potentialWinnings: potentialWinningsValue.toFixed(2),
        };
    }, [legs, stake]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsError(false);
        setMessage('');

        if (legs.length === 0) {
            setIsError(true);
            setMessage('You must add at least one bet leg.');
            return;
        }

        try {
            const betRequests = legs.map(leg => ({
                sport: leg.sport,
                eventName: leg.eventName,
                eventDate: leg.eventDate,
                marketType: leg.marketType,
                selection: leg.selection,
                odds: parseFloat(leg.odds),
                bookmaker: leg.bookmaker,
                stake: parseFloat(stake),
            }));

            await addBet({ bets: betRequests });

            setMessage('Bet placed successfully!');
            setIsError(false);
            setLegs([createInitialLeg()]);
            setStake('');

            setTimeout(() => setMessage(''), 3000);
        } catch (error) {
            setIsError(true);
            const errorMsg = error.response?.data?.message || 'Failed to place bet. Please check the fields.';
            setMessage(errorMsg);
            console.error('Error adding bet:', error);
        }
    };

    const handleImported = (importedBet) => {
        navigate('/bets', {
            state: {
                flashMessage: `Imported bet: ${importedBet.eventName} (${importedBet.selection})`,
            },
        });
    };

    return (
        <div className="surface-card max-w-4xl mx-auto animate-page-enter">
            <header className="mb-8 border-b border-hairline pb-6">
                <h2 className="display-sm">Place a New Bet</h2>
                <p className="body-sm text-muted mt-2">Import a slip from screenshot or fill in the details manually.</p>
            </header>

            <section className="mb-10 rounded-lg border border-hairline bg-surface-soft/50 p-6 md:p-8 space-y-4">
                <div>
                    <h3 className="caption-uppercase text-on-dark">Import from screenshot</h3>
                    <p className="body-sm text-muted mt-2">
                        Paste a screenshot (Ctrl+V), drag an image, or choose a file — AI will read the event, pick, and odds.
                    </p>
                </div>
                <ImportBetFromImage onImported={handleImported} />
            </section>

            <SectionDivider label="or enter manually" />

            <form onSubmit={handleSubmit} className="mt-8 space-y-6 stagger-children">
                {legs.map((leg, index) => (
                    <BetLegCard
                        key={leg.id}
                        leg={leg}
                        index={index}
                        totalLegs={legs.length}
                        onChange={handleLegChange}
                        onRemove={removeLeg}
                    />
                ))}

                <button
                    type="button"
                    onClick={addLeg}
                    className="flex w-full items-center justify-center gap-2 rounded-lg border-2 border-dashed border-hairline py-4 text-sm font-bold uppercase tracking-widest text-muted transition-all hover:border-primary/50 hover:bg-surface-soft hover:text-on-dark"
                >
                    <span className="text-lg leading-none text-primary">+</span>
                    Add another leg (parlay)
                </button>

                <section className="space-y-4">
                    <h3 className="field-label !mb-0">Order summary</h3>
                    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                        <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-hairline bg-surface-card p-4">
                            <label htmlFor="stake" className="field-label !mb-0 !text-[10px] text-center">Stake amount</label>
                            <div className="relative w-full">
                                <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted">$</span>
                                <input
                                    type="number"
                                    id="stake"
                                    value={stake}
                                    onChange={(e) => setStake(e.target.value)}
                                    required
                                    min="0.01"
                                    step="0.01"
                                    placeholder="0.00"
                                    className="input-field !h-auto w-full py-2 pl-7 text-center text-base font-bold text-primary font-numeric"
                                />
                            </div>
                        </div>

                        <div className="flex flex-col items-center justify-center gap-1 rounded-lg border border-hairline bg-surface-card p-4">
                            <p className="field-label !mb-0 !text-[10px]">Total odds</p>
                            <p className="text-2xl font-bold text-on-dark font-numeric">{totalOdds}</p>
                        </div>

                        <div className="flex flex-col items-center justify-center gap-1 rounded-lg border border-hairline bg-surface-card p-4">
                            <p className="field-label !mb-0 !text-[10px]">Potential return</p>
                            <p className="text-2xl font-bold text-primary font-numeric">
                                {potentialWinnings === '—' ? '—' : `$${potentialWinnings}`}
                            </p>
                        </div>
                    </div>
                </section>

                <button
                    type="submit"
                    className="button-primary w-full !h-14 text-lg"
                >
                    {legs.length > 1 ? 'Place parlay order' : 'Place single order'}
                </button>
            </form>

            {message && (
                <div className={`mt-8 flex items-center gap-3 rounded-lg border p-4 ${isError ? 'border-accent-rose/50 bg-accent-rose/10 text-accent-rose' : 'border-primary/50 bg-primary/10 text-primary'}`}>
                    <span className="text-xl" aria-hidden="true">{isError ? '!' : '✓'}</span>
                    <p className="text-sm font-bold uppercase tracking-wider">{message}</p>
                </div>
            )}
        </div>
    );
};

export default AddBetForm;
