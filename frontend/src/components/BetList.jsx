import React, { useState, useEffect, useMemo } from 'react';
import { getBets, settleBet, deleteBet, updateBet } from '../api';

const EditBetModal = ({ bet, isOpen, onClose, onSave }) => {
    const [formData, setFormData] = useState({ ...bet });

    useEffect(() => {
        if (bet) {
            setFormData({ ...bet });
        }
    }, [bet]);

    if (!isOpen) return null;

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === 'stake' || name === 'odds' ? parseFloat(value) : value
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        onSave(bet.id, formData);
    };

    return (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full flex items-center justify-center z-50">
            <div className="relative bg-white rounded-lg shadow-xl p-8 max-w-md w-full">
                <h3 className="text-xl font-semibold mb-4 text-gray-900">Edit Bet</h3>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Event Name</label>
                        <input type="text" name="eventName" value={formData.eventName || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                         <div>
                            <label className="block text-sm font-medium text-gray-700">Selection</label>
                            <input type="text" name="selection" value={formData.selection || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Market</label>
                            <input type="text" name="marketType" value={formData.marketType || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
                        </div>
                    </div>
                   
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Stake</label>
                            <input type="number" name="stake" step="0.01" value={formData.stake || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Odds</label>
                            <input type="number" name="odds" step="0.01" value={formData.odds || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
                        </div>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Bookmaker</label>
                        <input type="text" name="bookmaker" value={formData.bookmaker || ''} onChange={handleChange} className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
                    </div>
                    <div className="flex justify-end space-x-3 mt-6">
                        <button type="button" onClick={onClose} className="px-4 py-2 bg-gray-200 text-gray-800 rounded hover:bg-gray-300">Cancel</button>
                        <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Save Changes</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

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

const BetRow = ({ bet, onSettle, onDelete, onEdit, isChild = false }) => (
    <tr className={isChild ? 'bg-gray-50' : 'bg-white hover:bg-gray-50 transition-colors'}>
        <td className={`px-6 py-4 whitespace-nowrap text-sm ${isChild ? 'pl-10' : ''}`}>
            <div className="font-medium text-gray-900">{bet.eventName}</div>
            <div className="text-gray-500 text-xs">{bet.sport} - {bet.marketType}</div>
            <div className="text-blue-600 font-semibold">{bet.selection}</div>
        </td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{bet.bookmaker}</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{bet.stake ? `$${bet.stake.toFixed(2)}` : ''}</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{bet.odds ? bet.odds.toFixed(2) : ''}</td>
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
            {bet.potentialWinnings ? `$${bet.potentialWinnings.toFixed(2)}` : ''}
        </td>
        <td className="px-6 py-4 whitespace-nowrap text-sm">
            <div className="flex flex-col space-y-2">
                <BetStatusBadge status={bet.status} />
                {!isChild && bet.status === 'PENDING' && (
                    <div className="flex space-x-1 mt-1">
                         <button 
                            onClick={() => onSettle(bet.id, 'WON')}
                            title="Mark as Won"
                            className="p-1 bg-green-100 text-green-700 rounded hover:bg-green-200 transition-colors text-xs"
                        >
                            ✅
                        </button>
                        <button 
                            onClick={() => onSettle(bet.id, 'LOST')}
                            title="Mark as Lost"
                            className="p-1 bg-red-100 text-red-700 rounded hover:bg-red-200 transition-colors text-xs"
                        >
                            ❌
                        </button>
                        <button 
                            onClick={() => onSettle(bet.id, 'VOID')}
                            title="Mark as Void"
                            className="p-1 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition-colors text-xs"
                        >
                            🔄
                        </button>
                    </div>
                )}
            </div>
        </td>
        <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold">
            {bet.status !== 'PENDING' ? (
                <span className={bet.finalProfit > 0 ? 'text-green-600' : bet.finalProfit < 0 ? 'text-red-600' : 'text-gray-600'}>
                    {bet.finalProfit !== null ? (bet.finalProfit >= 0 ? `+$${bet.finalProfit.toFixed(2)}` : `-$${Math.abs(bet.finalProfit).toFixed(2)}`) : 'N/A'}
                </span>
            ) : (
                <div className="flex space-x-2">
                    <button onClick={() => onEdit(bet)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                    <button onClick={() => onDelete(bet.id)} className="text-red-500 hover:text-red-700 text-xs font-medium">Delete</button>
                </div>
            )}
        </td>
    </tr>
);


const BetList = () => {
    const [bets, setBets] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingBet, setEditingBet] = useState(null);

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

    useEffect(() => {
        fetchBets();
    }, []);

    const handleSettle = async (id, status) => {
        try {
            await settleBet(id, status);
            fetchBets();
        } catch (err) {
            alert('Failed to settle bet. Please try again.');
            console.error(err);
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this bet? This action cannot be undone.')) {
            try {
                await deleteBet(id);
                fetchBets();
            } catch (err) {
                alert('Failed to delete bet.');
                console.error(err);
            }
        }
    };

    const handleEditClick = (bet) => {
        setEditingBet(bet);
        setIsEditModalOpen(true);
    };

    const handleSaveEdit = async (id, updatedData) => {
        try {
            await updateBet(id, updatedData);
            setIsEditModalOpen(false);
            setEditingBet(null);
            fetchBets();
        } catch (err) {
            alert('Failed to update bet.');
            console.error(err);
        }
    };
    
    // Filter out child bets to only show parent (PARLAY) and single bets
    const topLevelBets = useMemo(() => {
        return bets.filter(bet => bet.betType !== 'SINGLE' || !bet.parentBet);
    }, [bets]);


    if (loading) {
        return <p className="text-center text-gray-500 mt-8">Loading bets...</p>;
    }

    if (error) {
        return <p className="text-center text-red-500 mt-8">{error}</p>;
    }

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                 <h2 className="text-2xl font-bold text-gray-800">My Bets</h2>
            </div>
           
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
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                            {topLevelBets.map((bet) => (
                                <React.Fragment key={bet.id}>
                                    <BetRow 
                                        bet={bet} 
                                        onSettle={handleSettle} 
                                        onDelete={handleDelete}
                                        onEdit={handleEditClick}
                                    />
                                    {bet.betType === 'PARLAY' && bet.childBets && bet.childBets.map(childBet => (
                                        <BetRow 
                                            bet={childBet} 
                                            key={childBet.id} 
                                            isChild={true} 
                                            onSettle={handleSettle}
                                            // Child bets can also be edited/deleted if logic permits, but for now kept read-only for simplicity
                                            onDelete={() => {}} 
                                            onEdit={() => {}} 
                                        />
                                    ))}
                                </React.Fragment>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            
            <EditBetModal 
                isOpen={isEditModalOpen}
                bet={editingBet}
                onClose={() => setIsEditModalOpen(false)}
                onSave={handleSaveEdit}
            />
        </div>
    );
};

export default BetList;