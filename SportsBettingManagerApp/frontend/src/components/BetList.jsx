import React, { useState, useEffect } from 'react';
import { getBets } from '../api';

const BetList = () => {
    const [bets, setBets] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchBets = async () => {
            try {
                setLoading(true);
                const response = await getBets();
                setBets(response.data);
                setError(null);
            } catch (err) {
                setError('Could not fetch bets. Please try again later.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchBets();
    }, []);

    if (loading) {
        return <p className="text-center text-gray-500">Loading bets...</p>;
    }

    if (error) {
        return <p className="text-center text-red-500">{error}</p>;
    }

    return (
        <div>
            <h2 className="text-2xl font-bold mb-6 text-gray-800">My Bets</h2>
            {bets.length === 0 ? (
                <p className="text-center text-gray-500">You haven't placed any bets yet.</p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white border border-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Event</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stake</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Odds</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Potential Win</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                            {bets.map((bet) => (
                                <tr key={bet.id}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{bet.eventName}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{typeof bet.stake === 'number' ? `${bet.stake.toFixed(2)}` : 'N/A'}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{typeof bet.odds === 'number' ? bet.odds.toFixed(2) : 'N/A'}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${bet.status === 'WON' ? 'bg-green-100 text-green-800' : bet.status === 'LOST' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800'}`}>
                                            {bet.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${(bet.stake * bet.odds).toFixed(2)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default BetList;
