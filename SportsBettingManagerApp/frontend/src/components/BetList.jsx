import React, { useState, useEffect, useMemo } from 'react';
import { getBets } from '../api';

const BetStatusBadge = ({ status }) => {
    const statusClasses = {
        PENDING: 'bg-yellow-100 text-yellow-800',
        WON: 'bg-green-100 text-green-800',
        LOST: 'bg-red-100 text-red-800',
        VOID: 'bg-gray-100 text-gray-800',
        CASHED_OUT: 'bg-blue-100 text-blue-800',
        HALF_WON: 'bg-green-100 text-green-600',
        HALF_LOST: 'bg-red-100 text-red-600',
    };

    return (
        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${statusClasses[status] || 'bg-gray-100 text-gray-800'}`}>
            {status}
        </span>
    );
};

const BetRow = ({ bet, isChild = false }) => (
    <tr className={isChild ? 'bg-gray-50' : 'bg-white'}>
        <td className={`px-6 py-4 whitespace-nowrap text-sm ${isChild ? 'pl-10' : ''}`}>
            <div className="font-medium text-gray-900">{bet.eventName}</div>
            <div className="text-gray-500">{bet.sport} - {bet.marketType}</div>
            <div className="text-blue-600 font-semibold">{bet.selection}</div>
        </td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{bet.bookmaker}</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{bet.stake ? `$${bet.stake.toFixed(2)}` : 'N/A'}</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{bet.odds ? bet.odds.toFixed(2) : 'N/A'}</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
            {bet.potentialWinnings ? `$${bet.potentialWinnings.toFixed(2)}` : 'N/A'}
        </td>
        <td className="px-6 py-4 whitespace-nowrap text-sm">
            <BetStatusBadge status={bet.status} />
        </td>
        <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold">
            <span className={bet.finalProfit > 0 ? 'text-green-600' : bet.finalProfit < 0 ? 'text-red-600' : 'text-gray-600'}>
                {bet.finalProfit !== null ? `$${bet.finalProfit.toFixed(2)}` : 'N/A'}
            </span>
        </td>
    </tr>
);


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
    
    // Filter out child bets to only show parent (PARLAY) and single bets
    const topLevelBets = useMemo(() => {
        return bets.filter(bet => bet.betType !== 'SINGLE' || !bet.parentBet);
    }, [bets]);


    if (loading) {
        return <p className="text-center text-gray-500">Loading bets...</p>;
    }

    if (error) {
        return <p className="text-center text-red-500">{error}</p>;
    }

    return (
        <div>
            <h2 className="text-2xl font-bold mb-6 text-gray-800">My Bets</h2>
            {topLevelBets.length === 0 ? (
                <p className="text-center text-gray-500">You haven't placed any bets yet.</p>
            ) : (
                <div className="overflow-x-auto shadow-md rounded-lg">
                    <table className="min-w-full bg-white border border-gray-200">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Event & Selection</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Bookmaker</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stake</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Odds</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Potential Win</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Profit/Loss</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                            {topLevelBets.map((bet) => (
                                <React.Fragment key={bet.id}>
                                    <BetRow bet={bet} />
                                    {bet.betType === 'PARLAY' && bet.childBets && bet.childBets.map(childBet => (
                                        <BetRow bet={childBet} key={childBet.id} isChild={true} />
                                    ))}
                                </React.Fragment>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default BetList;
