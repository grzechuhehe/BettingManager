import { useState, useEffect } from 'react';
import { getProfilePicks } from '../../api';

export default function ProfilePicksView({ xUsername, onBack }) {
    const [picks, setPicks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchPicks = async () => {
            try {
                const res = await getProfilePicks(xUsername);
                setPicks(res.data);
            } catch (err) {
                console.error("Failed to load picks", err);
                setError('Failed to load picks for this profile.');
            } finally {
                setLoading(false);
            }
        };
        fetchPicks();
    }, [xUsername]);

    if (loading) return <p className="p-4 text-gray-600">Loading picks...</p>;
    if (error) return <p className="p-4 text-red-600">{error}</p>;

    return (
        <div className="bg-white p-6 rounded-lg shadow-md">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-800">Picks by @{xUsername}</h2>
                <button onClick={onBack} className="text-gray-600 hover:text-gray-900 font-medium">
                    &larr; Back to Search
                </button>
            </div>

            {picks.length === 0 ? (
                <p className="text-gray-500 italic">No bets found for this profile yet.</p>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {picks.map(pick => (
                        <div key={pick.id} className="border border-gray-200 rounded-lg overflow-hidden flex flex-col">
                            {pick.imageProofPath && (
                                <img 
                                    src={`https://localhost:8443${pick.imageProofPath}`} 
                                    alt="Bet slip proof" 
                                    className="w-full h-48 object-cover object-top border-b border-gray-200"
                                    onError={(e) => e.target.style.display = 'none'}
                                />
                            )}
                            <div className="p-4 flex-1 flex flex-col justify-between">
                                <div>
                                    <h3 className="font-bold text-lg mb-1">{pick.eventName || 'Unknown Event'}</h3>
                                    <p className="text-sm text-gray-600 mb-2">Selection: <span className="font-semibold text-gray-800">{pick.selection}</span></p>
                                    <div className="flex justify-between items-center text-sm">
                                        <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded font-bold">Odds: {pick.odds}</span>
                                        {pick.units && <span className="bg-green-100 text-green-800 px-2 py-1 rounded font-bold">{pick.units} u</span>}
                                        {pick.stake && <span className="bg-yellow-100 text-yellow-800 px-2 py-1 rounded font-bold">Stake: {pick.stake}</span>}
                                    </div>
                                </div>
                                <div className="mt-4 pt-3 border-t border-gray-100 flex justify-between text-xs text-gray-500">
                                    <span>{pick.bookmaker || 'Unknown Bookie'}</span>
                                    <span>{new Date(pick.placedAt).toLocaleDateString()}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
