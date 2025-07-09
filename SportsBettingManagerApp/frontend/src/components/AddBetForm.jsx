import React, { useState } from 'react';
import { addBet } from '../api';
import { useNavigate } from 'react-router-dom';

const AddBetForm = () => {
    const navigate = useNavigate();
    const [amount, setAmount] = useState('');
    const [odds, setOdds] = useState('');
    const [betType, setBetType] = useState('WIN'); // Default to WIN
    const [homeTeam, setHomeTeam] = useState('');
    const [awayTeam, setAwayTeam] = useState('');
    const [sportType, setSportType] = useState('FOOTBALL'); // Default to FOOTBALL
    const [eventDate, setEventDate] = useState(''); // New state for event date
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);

    const betTypes = ['WIN', 'DRAW', 'OVER', 'UNDER'];
    const sportTypes = ['FOOTBALL', 'TENIS', 'ICEHOKEY', 'FORMULA1', 'HANDBALL', 'AMERICANFOOTBALL', 'VOLLEYBALL', 'BASKETBALL', 'MMA', 'BOXING', 'CS', 'LOL', 'POLITICS', 'SHOWBIZNES'];

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsError(false);
        setMessage('');

        try {
            const betData = {
                amount: parseFloat(amount),
                odds: parseFloat(odds),
                type: betType,
                status: 'PENDING', // Default status for new bets
                event: {
                    teamHome: homeTeam,
                    teamAway: awayTeam,
                    sportType: sportType,
                    date: eventDate, // Include the event date
                }
            };

            // The backend expects the user object to be part of the Bet object,
            // but it should be automatically handled by Spring Security based on the JWT.
            // So, we don't need to send the user object from the frontend.

            const response = await addBet(betData);
            setMessage('Bet added successfully!');
            // Clear form
            setAmount('');
            setOdds('');
            setBetType('WIN');
            setHomeTeam('');
            setAwayTeam('');
            setSportType('FOOTBALL');
            setEventDate(''); // Clear event date
            navigate('/dashboard'); // Redirect to dashboard after adding bet
        } catch (error) {
            setIsError(true);
            if (error.response && error.response.data) {
                setMessage(error.response.data.error || 'Failed to add bet.');
            } else {
                setMessage('An error occurred while connecting to the server.');
            }
            console.error('Error adding bet:', error);
        }
    };

    return (
        <div className="p-4">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Add New Bet</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                    <label htmlFor="amount" className="block text-sm font-medium text-gray-700">Amount</label>
                    <input
                        type="number"
                        id="amount"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        required
                        min="0.01"
                        step="0.01"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                    />
                </div>
                <div>
                    <label htmlFor="odds" className="block text-sm font-medium text-gray-700">Odds</label>
                    <input
                        type="number"
                        id="odds"
                        value={odds}
                        onChange={(e) => setOdds(e.target.value)}
                        required
                        min="1.01"
                        step="0.01"
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                    />
                </div>
                <div>
                    <label htmlFor="betType" className="block text-sm font-medium text-gray-700">Bet Type</label>
                    <select
                        id="betType"
                        value={betType}
                        onChange={(e) => setBetType(e.target.value)}
                        required
                        className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                    >
                        {betTypes.map(type => (
                            <option key={type} value={type}>{type}</option>
                        ))}
                    </select>
                </div>
                <fieldset className="border p-4 rounded-md space-y-4">
                    <legend className="text-lg font-medium text-gray-900">Sport Event Details</legend>
                    <div>
                        <label htmlFor="homeTeam" className="block text-sm font-medium text-gray-700">Home Team</label>
                        <input
                            type="text"
                            id="homeTeam"
                            value={homeTeam}
                            onChange={(e) => setHomeTeam(e.target.value)}
                            required
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                        />
                    </div>
                    <div>
                        <label htmlFor="awayTeam" className="block text-sm font-medium text-gray-700">Away Team</label>
                        <input
                            type="text"
                            id="awayTeam"
                            value={awayTeam}
                            onChange={(e) => setAwayTeam(e.target.value)}
                            required
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                        />
                    </div>
                    <div>
                        <label htmlFor="sportType" className="block text-sm font-medium text-gray-700">Sport Type</label>
                        <select
                            id="sportType"
                            value={sportType}
                            onChange={(e) => setSportType(e.target.value)}
                            required
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                        >
                            {sportTypes.map(type => (
                                <option key={type} value={type}>{type}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="eventDate" className="block text-sm font-medium text-gray-700">Event Date and Time</label>
                        <input
                            type="datetime-local"
                            id="eventDate"
                            value={eventDate}
                            onChange={(e) => setEventDate(e.target.value)}
                            required
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                        />
                    </div>
                </fieldset>
                <button
                    type="submit"
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                >
                    Add Bet
                </button>
            </form>
            {message && (
                <p className={`mt-4 text-center ${isError ? 'text-red-500' : 'text-green-500'}`}>
                    {message}
                </p>
            )}
        </div>
    );
};

export default AddBetForm;
