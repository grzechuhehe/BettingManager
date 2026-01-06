import React, { useState, useEffect, useMemo } from 'react';
import { addBet } from '../api';
import { useNavigate } from 'react-router-dom';

const initialLegState = {
    sport: 'Football',
    eventName: '',
    eventDate: '',
    marketType: 'MONEYLINE_1X2',
    selection: '',
    odds: '',
    bookmaker: 'STS',
};

const marketTypes = ['MONEYLINE_1X2', 'MONEYLINE_12', 'TOTALS_OVER_UNDER', 'HANDICAP', 'ASIAN_HANDICAP', 'CORRECT_SCORE', 'PLAYER_PROPS', 'BOTH_TEAMS_TO_SCORE', 'OUTRIGHT', 'OTHER'];
const bookmakers = ['STS', 'Fortuna', 'Betclic', 'Superbet', 'forBET', 'eTOTO', 'LVBET', 'Totalbet', 'Betfan', 'Fuksiarz', 'Betters', 'GoBet'];
const sports = ['Football', 'Basketball', 'Tennis', 'Ice Hockey', 'MMA', 'Boxing', 'F1', 'CS:GO', 'LoL', 'Valorant', 'Other'];

const AddBetForm = () => {
    const navigate = useNavigate();
    const [legs, setLegs] = useState([initialLegState]);
    const [stake, setStake] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);

    const handleLegChange = (index, e) => {
        const { name, value } = e.target;
        const newLegs = [...legs];
        newLegs[index] = { ...newLegs[index], [name]: value };
        setLegs(newLegs);
    };

    const addLeg = () => {
        setLegs([...legs, initialLegState]);
    };

    const removeLeg = (index) => {
        const newLegs = legs.filter((_, i) => i !== index);
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
                ...leg,
                odds: parseFloat(leg.odds),
                // For a parlay, the backend uses the stake from the first leg,
                // so we ensure each leg has the stake value.
                stake: parseFloat(stake)
            }));

            const createBetRequest = {
                bets: betRequests,
            };

            await addBet(createBetRequest);

            setMessage('Bet placed successfully!');
            setTimeout(() => navigate('/dashboard'), 1500); // Redirect after a short delay

        } catch (error) {
            setIsError(true);
            const errorMsg = error.response?.data?.message || 'Failed to place bet. Please check the fields.';
            setMessage(errorMsg);
            console.error('Error adding bet:', error);
        }
    };

    return (
        <div className="p-4 max-w-4xl mx-auto bg-white rounded-lg shadow-md">
            <h2 className="text-3xl font-bold mb-6 text-gray-800 border-b pb-2">Place a New Bet</h2>
            
            <form onSubmit={handleSubmit} className="space-y-6">
                {legs.map((leg, index) => (
                    <div key={index} className="p-4 border rounded-lg relative space-y-4 bg-gray-50">
                        <h3 className="text-lg font-semibold text-gray-700">Leg #{index + 1}</h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {/* Left Column */}
                            <div>
                                <label htmlFor={`bookmaker-${index}`} className="block text-sm font-medium text-gray-700">Bookmaker</label>
                                <select name="bookmaker" value={leg.bookmaker} onChange={e => handleLegChange(index, e)} className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900">
                                    {bookmakers.map(b => <option key={b} value={b}>{b}</option>)}
                                </select>
                            </div>
                            <div>
                                <label htmlFor={`sport-${index}`} className="block text-sm font-medium text-gray-700">Sport</label>
                                <select name="sport" value={leg.sport} onChange={e => handleLegChange(index, e)} className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900">
                                    {sports.map(s => <option key={s} value={s}>{s}</option>)}
                                </select>
                            </div>
                            <div>
                                <label htmlFor={`eventName-${index}`} className="block text-sm font-medium text-gray-700">Event Name</label>
                                <input type="text" name="eventName" placeholder="e.g., Real Madrid vs Barcelona" value={leg.eventName} onChange={e => handleLegChange(index, e)} required className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900"/>
                            </div>
                            <div>
                                <label htmlFor={`eventDate-${index}`} className="block text-sm font-medium text-gray-700">Event Date</label>
                                <input type="datetime-local" name="eventDate" value={leg.eventDate} onChange={e => handleLegChange(index, e)} required className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900"/>
                            </div>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            {/* Right Column */}
                            <div>
                                <label htmlFor={`marketType-${index}`} className="block text-sm font-medium text-gray-700">Market</label>
                                <select name="marketType" value={leg.marketType} onChange={e => handleLegChange(index, e)} className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900">
                                    {marketTypes.map(mt => <option key={mt} value={mt}>{mt}</option>)}
                                </select>
                            </div>
                             <div>
                                <label htmlFor={`selection-${index}`} className="block text-sm font-medium text-gray-700">Selection</label>
                                <input type="text" name="selection" placeholder="e.g., Real Madrid to win" value={leg.selection} onChange={e => handleLegChange(index, e)} required className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900"/>
                            </div>
                            <div>
                                <label htmlFor={`odds-${index}`} className="block text-sm font-medium text-gray-700">Odds</label>
                                <input type="number" name="odds" placeholder="1.85" step="0.01" min="1.01" value={leg.odds} onChange={e => handleLegChange(index, e)} required className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900"/>
                            </div>
                        </div>

                        {legs.length > 1 && (
                            <button type="button" onClick={() => removeLeg(index)} className="absolute top-2 right-2 p-1 bg-red-500 text-white rounded-full text-xs">
                                &#x2715;
                            </button>
                        )}
                    </div>
                ))}

                <button type="button" onClick={addLeg} className="w-full py-2 px-4 border border-dashed border-gray-400 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-100">
                    + Add Another Leg (for Parlay)
                </button>

                <div className="p-4 border-t mt-6 space-y-4">
                     <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-center">
                        <div>
                            <label htmlFor="stake" className="block text-2xl font-medium text-gray-700">Stake</label>
                            <input
                                type="number"
                                id="stake"
                                value={stake}
                                onChange={(e) => setStake(e.target.value)}
                                required
                                min="0.01"
                                step="0.01"
                                placeholder="Total amount"
                                className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm text-gray-900 text-xl"
                            />
                        </div>
                        <div className="text-center">
                            <p className="text-sm font-medium text-gray-500">Total Odds</p>
                            <p className="text-2xl font-bold text-gray-800">{totalOdds}</p>
                        </div>
                        <div className="text-center">
                            <p className="text-sm font-medium text-gray-500">Potential Winnings</p>
                            <p className="text-2xl font-bold text-green-600">${potentialWinnings}</p>
                        </div>
                    </div>
                </div>

                <button
                    type="submit"
                    className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-lg font-medium text-white bg-blue-600 hover:bg-blue-700"
                >
                    {legs.length > 1 ? 'Place Parlay Bet' : 'Place Single Bet'}
                </button>
            </form>

            {message && (
                <p className={`mt-4 text-center p-2 rounded ${isError ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                    {message}
                </p>
            )}
        </div>
    );
};

export default AddBetForm;
