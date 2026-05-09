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
        <div className="fixed inset-0 bg-canvas/90 backdrop-blur-sm overflow-y-auto h-full w-full flex items-center justify-center z-50">
            <div className="relative bg-surface-card rounded-lg border border-hairline p-10 max-w-md w-full shadow-2xl">
                <h3 className="display-sm mb-6">Edit Order</h3>
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="block text-[10px] font-bold text-muted uppercase tracking-widest mb-2">Event Name</label>
                        <input type="text" name="eventName" value={formData.eventName || ''} onChange={handleChange} className="input-field" />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                         <div>
                            <label className="block text-[10px] font-bold text-muted uppercase tracking-widest mb-2">Selection</label>
                            <input type="text" name="selection" value={formData.selection || ''} onChange={handleChange} className="input-field" />
                        </div>
                        <div>
                            <label className="block text-[10px] font-bold text-muted uppercase tracking-widest mb-2">Market</label>
                            <input type="text" name="marketType" value={formData.marketType || ''} onChange={handleChange} className="input-field" />
                        </div>
                    </div>
                   
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-[10px] font-bold text-muted uppercase tracking-widest mb-2">Stake</label>
                            <input type="number" name="stake" step="0.01" value={formData.stake || ''} onChange={handleChange} className="input-field" />
                        </div>
                        <div>
                            <label className="block text-[10px] font-bold text-muted uppercase tracking-widest mb-2">Odds</label>
                            <input type="number" name="odds" step="0.01" value={formData.odds || ''} onChange={handleChange} className="input-field" />
                        </div>
                    </div>
                    <div>
                        <label className="block text-[10px] font-bold text-muted uppercase tracking-widest mb-2">Bookmaker</label>
                        <input type="text" name="bookmaker" value={formData.bookmaker || ''} onChange={handleChange} className="input-field" />
                    </div>
                    <div className="flex justify-end space-x-4 mt-8">
                        <button type="button" onClick={onClose} className="button-secondary text-sm">Cancel</button>
                        <button type="submit" className="button-primary text-sm">Save Changes</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

const BetStatusBadge = ({ status }) => {
    const statusClasses = {
        PENDING: 'bg-primary/20 text-primary border-primary/30',
        WON: 'bg-emerald-500/20 text-emerald-500 border-emerald-500/30',
        LOST: 'bg-rose-500/20 text-rose-500 border-rose-500/30',
        VOID: 'bg-muted/20 text-muted border-muted/30',
        CASHED_OUT: 'bg-blue-500/20 text-blue-500 border-blue-500/30',
        HALF_WON: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
        HALF_LOST: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
    };

    return (
        <span className={`px-2.5 py-0.5 inline-flex text-[10px] leading-4 font-black rounded-full border uppercase tracking-widest ${statusClasses[status] || 'bg-hairline text-muted border-hairline'}`}>
            {status}
        </span>
    );
};

const BetRow = ({ bet, onSettle, onDelete, onEdit, isChild = false }) => (
    <tr className={`${isChild ? 'bg-surface-soft/30' : 'bg-surface-card hover:bg-surface-elevated/50 transition-colors'} border-b border-hairline`}>
        <td className={`px-6 py-5 whitespace-nowrap ${isChild ? 'pl-12' : ''}`}>
            <div className="font-bold text-on-dark text-sm">{bet.eventName}</div>
            <div className="text-muted text-[10px] font-bold uppercase tracking-wider mt-0.5">{bet.sport} · {bet.marketType}</div>
            <div className="text-primary font-bold text-xs mt-1">{bet.selection}</div>
        </td>
        <td className="px-6 py-5 whitespace-nowrap text-xs font-bold text-body uppercase tracking-wider">{bet.bookmaker}</td>
        <td className="px-6 py-5 whitespace-nowrap text-sm font-bold text-on-dark font-numeric">{bet.stake ? `$${bet.stake.toFixed(2)}` : ''}</td>
        <td className="px-6 py-5 whitespace-nowrap text-sm font-bold text-on-dark font-numeric">{bet.odds ? bet.odds.toFixed(2) : ''}</td>
        <td className="px-6 py-5 whitespace-nowrap text-sm font-bold text-primary font-numeric">
            {bet.potentialWinnings ? `$${bet.potentialWinnings.toFixed(2)}` : ''}
        </td>
        <td className="px-6 py-5 whitespace-nowrap text-sm">
            <div className="flex flex-col space-y-2">
                <BetStatusBadge status={bet.status} />
                {!isChild && bet.status === 'PENDING' && (
                    <div className="flex gap-2 mt-1">
                         <button 
                            onClick={() => onSettle(bet.id, 'WON')}
                            title="Mark as Won"
                            className="w-10 h-10 flex items-center justify-center bg-emerald-500/10 text-emerald-500 border border-emerald-500/20 rounded-md hover:bg-emerald-500 hover:text-white transition-all text-xs"
                        >
                            ✅
                        </button>
                        <button 
                            onClick={() => onSettle(bet.id, 'LOST')}
                            title="Mark as Lost"
                            className="w-10 h-10 flex items-center justify-center bg-rose-500/10 text-rose-500 border border-rose-500/20 rounded-md hover:bg-rose-500 hover:text-white transition-all text-xs"
                        >
                            ❌
                        </button>
                        <button 
                            onClick={() => onSettle(bet.id, 'VOID')}
                            title="Mark as Void"
                            className="w-10 h-10 flex items-center justify-center bg-surface-elevated text-muted border border-hairline rounded-md hover:bg-on-dark hover:text-canvas transition-all text-xs"
                        >
                            🔄
                        </button>
                    </div>
                )}
            </div>
        </td>
        <td className="px-6 py-5 whitespace-nowrap text-sm font-black font-numeric">
            {bet.status !== 'PENDING' ? (
                <span className={bet.finalProfit > 0 ? 'text-emerald-500' : bet.finalProfit < 0 ? 'text-rose-500' : 'text-muted'}>
                    {bet.finalProfit !== null ? (bet.finalProfit >= 0 ? `+$${bet.finalProfit.toFixed(2)}` : `-$${Math.abs(bet.finalProfit).toFixed(2)}`) : 'N/A'}
                </span>
            ) : (
                <div className="flex space-x-3">
                    <button onClick={() => onEdit(bet)} className="text-[10px] font-black uppercase tracking-widest text-muted hover:text-on-dark transition-colors">Edit</button>
                    <button onClick={() => onDelete(bet.id)} className="text-[10px] font-black uppercase tracking-widest text-rose-500 hover:text-rose-400 transition-colors">Delete</button>
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
            setError('Could not synchronize market data. Please retry.');
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
            alert('Failed to settle market position.');
            console.error(err);
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm('Confirm permanent deletion of this market record?')) {
            try {
                await deleteBet(id);
                fetchBets();
            } catch (err) {
                alert('Failed to delete market record.');
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
            alert('Failed to update market record.');
            console.error(err);
        }
    };
    
    // Filter out child bets to only show parent (PARLAY) and single bets
    const topLevelBets = useMemo(() => {
        return bets.filter(bet => bet.betType !== 'SINGLE' || !bet.parentBet);
    }, [bets]);


    if (loading) {
        return (
            <div className="flex flex-col items-center justify-center py-20 space-y-4">
                <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-xs font-bold text-muted uppercase tracking-widest">Synchronizing Ledger...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-rose-500/10 border border-rose-500/50 p-6 rounded-lg text-center">
                <p className="text-rose-500 font-bold uppercase tracking-wider">{error}</p>
            </div>
        );
    }

    return (
        <div className="space-y-8">
            <div className="flex justify-between items-center mb-2">
                 <h2 className="display-sm">Market Ledger</h2>
                 <p className="text-xs font-bold text-muted uppercase tracking-widest">{topLevelBets.length} Registered Positions</p>
            </div>
           
            {topLevelBets.length === 0 ? (
                <div className="surface-card py-20 text-center border-dashed">
                    <p className="text-muted font-bold uppercase tracking-widest">No active positions found in ledger.</p>
                </div>
            ) : (
                <div className="overflow-hidden border border-hairline rounded-lg">
                    <table className="min-w-full bg-canvas">
                        <thead>
                            <tr className="bg-surface-soft border-b border-hairline">
                                <th className="px-6 py-4 text-left text-[10px] font-black text-muted uppercase tracking-[0.2em]">Market & Parameters</th>
                                <th className="px-6 py-4 text-left text-[10px] font-black text-muted uppercase tracking-[0.2em]">Source</th>
                                <th className="px-6 py-4 text-left text-[10px] font-black text-muted uppercase tracking-[0.2em]">Stake</th>
                                <th className="px-6 py-4 text-left text-[10px] font-black text-muted uppercase tracking-[0.2em]">Odds</th>
                                <th className="px-6 py-4 text-left text-[10px] font-black text-muted uppercase tracking-[0.2em]">Potential P/L</th>
                                <th className="px-6 py-4 text-left text-[10px] font-black text-muted uppercase tracking-[0.2em]">Status</th>
                                <th className="px-6 py-4 text-left text-[10px] font-black text-muted uppercase tracking-[0.2em]">Settlement</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-hairline">
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