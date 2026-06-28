import React, { useState, useEffect, useMemo } from 'react';
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
const bookmakers = ['STS', 'Fortuna', 'Betclic', 'Superbet', 'forBET', 'eTOTO', 'LVBET', 'Totalbet', 'Betfan', 'Fuksiarz', 'Betters', 'GoBet'];
const sports = ['Football', 'Basketball', 'Tennis', 'Ice Hockey', 'MMA', 'Boxing', 'F1', 'CS:GO', 'LoL', 'Valorant', 'Other'];

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
        const newLegs = legs.filter(leg => leg.id !== id);
        setLegs(newLegs);
    };

    const { totalOdds, potentialWinnings } = useMemo(() => {
        if (legs.length === 0 || !stake) return { totalOdds: 0, potentialWinnings: 0 };
        const totalOddsValue = legs.reduce((acc, leg) => acc * (parseFloat(leg.odds) || 1), 1);
        const potentialWinningsValue = totalOddsValue * parseFloat(stake);
        return {
            totalOdds: totalOddsValue.toFixed(2),
            potentialWinnings: potentialWinningsValue.toFixed(2)
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
                stake: parseFloat(stake)
            }));

            const createBetRequest = {
                bets: betRequests,
            };

            await addBet(createBetRequest);

            setMessage('Bet placed successfully!');
            setIsError(false);
            setLegs([createInitialLeg()]);
            setStake('');

            setTimeout(() => {
                setMessage('');
            }, 3000);

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
                flashMessage: `Zaimportowano zakład: ${importedBet.eventName} (${importedBet.selection})`,
            },
        });
    };

    return (
        <div className="surface-card max-w-4xl mx-auto">
            <h2 className="display-sm mb-8 pb-4 border-b border-hairline">Place a New Bet</h2>

            <div className="mb-12 p-8 border border-hairline rounded-lg bg-surface-soft/50 space-y-4">
                <h3 className="text-sm font-bold text-on-dark uppercase tracking-widest">Import from Screenshot</h3>
                <p className="text-sm text-muted">Wklej zrzut ekranu (Ctrl+V), przeciągnij obraz albo wybierz plik — AI odczyta wydarzenie, typ i kurs.</p>
                <ImportBetFromImage onImported={handleImported} />
            </div>
            
            <form onSubmit={handleSubmit} className="space-y-8">
                {legs.map((leg, index) => (
                    <div key={leg.id} className="p-8 border border-hairline rounded-lg relative space-y-6 bg-surface-elevated/50">
                        <div className="flex items-center gap-3 mb-2">
                            <span className="w-6 h-6 bg-primary text-on-primary rounded-full flex items-center justify-center text-[10px] font-black uppercase tracking-tighter">Leg {index + 1}</span>
                            <h3 className="text-sm font-bold text-on-dark uppercase tracking-widest">Market Parameters</h3>
                        </div>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Bookmaker</label>
                                <select name="bookmaker" value={leg.bookmaker} onChange={e => handleLegChange(leg.id, e)} className="input-field">
                                    {bookmakers.map(b => <option key={b} value={b}>{b}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Sport</label>
                                <select name="sport" value={leg.sport} onChange={e => handleLegChange(leg.id, e)} className="input-field">
                                    {sports.map(s => <option key={s} value={s}>{s}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Event Name</label>
                                <input type="text" name="eventName" placeholder="e.g., Real Madrid vs Barcelona" value={leg.eventName} onChange={e => handleLegChange(leg.id, e)} required className="input-field"/>
                            </div>
                            <div>
                                <label className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Event Date</label>
                                <input type="datetime-local" name="eventDate" value={leg.eventDate} onChange={e => handleLegChange(leg.id, e)} required className="input-field"/>
                            </div>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            <div>
                                <label className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Market</label>
                                <select name="marketType" value={leg.marketType} onChange={e => handleLegChange(leg.id, e)} className="input-field">
                                    {marketTypes.map(mt => <option key={mt} value={mt}>{mt}</option>)}
                                </select>
                            </div>
                             <div>
                                <label className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Selection</label>
                                <input type="text" name="selection" placeholder="e.g., Real Madrid to win" value={leg.selection} onChange={e => handleLegChange(leg.id, e)} required className="input-field"/>
                            </div>
                            <div>
                                <label className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">Odds</label>
                                <input type="number" name="odds" placeholder="1.85" step="0.01" min="1.01" value={leg.odds} onChange={e => handleLegChange(leg.id, e)} required className="input-field"/>
                            </div>
                        </div>

                        {legs.length > 1 && (
                            <button type="button" onClick={() => removeLeg(leg.id)} className="absolute top-4 right-4 w-6 h-6 flex items-center justify-center bg-rose-500/10 text-rose-500 hover:bg-rose-500 hover:text-white rounded-md transition-all">
                                &#x2715;
                            </button>
                        )}
                    </div>
                ))}

                <button type="button" onClick={addLeg} className="w-full py-4 border-2 border-dashed border-hairline rounded-lg text-sm font-bold text-muted hover:text-on-dark hover:border-primary/50 hover:bg-surface-soft transition-all uppercase tracking-widest">
                    + Add Another Leg (for Parlay)
                </button>

                <div className="p-8 border border-hairline rounded-lg bg-surface-soft/50 mt-12 space-y-6">
                     <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-center">
                        <div>
                            <label htmlFor="stake" className="block text-xs font-bold text-muted uppercase tracking-[0.2em] mb-3">Stake Amount</label>
                            <input
                                type="number"
                                id="stake"
                                value={stake}
                                onChange={(e) => setStake(e.target.value)}
                                required
                                min="0.01"
                                step="0.01"
                                placeholder="Total amount"
                                className="input-field !text-2xl !h-14 font-bold text-primary font-numeric"
                            />
                        </div>
                        <div className="text-center md:text-left md:pl-8 md:border-l border-hairline">
                            <p className="text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-1">Total Odds</p>
                            <p className="text-3xl font-black text-on-dark font-numeric">{totalOdds}</p>
                        </div>
                        <div className="text-center md:text-left md:pl-8 md:border-l border-hairline">
                            <p className="text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-1">Potential P/L</p>
                            <p className="text-3xl font-black text-primary font-numeric">${potentialWinnings}</p>
                        </div>
                    </div>
                </div>

                <button
                    type="submit"
                    className="button-primary w-full !h-14 text-lg"
                >
                    {legs.length > 1 ? 'Place Parlay Order' : 'Place Single Order'}
                </button>
            </form>

            {message && (
                <div className={`mt-8 p-4 rounded-lg border flex items-center gap-3 ${isError ? 'bg-rose-500/10 border-rose-500/50 text-rose-500' : 'bg-primary/10 border-primary/50 text-primary'}`}>
                    <span className="text-xl">{isError ? '⚠️' : '✅'}</span>
                    <p className="font-bold text-sm uppercase tracking-wider">{message}</p>
                </div>
            )}
        </div>
    );
};

export default AddBetForm;
