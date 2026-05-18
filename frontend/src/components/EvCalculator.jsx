import React, { useState } from 'react';
import { calculateExpectedValue } from '../api';

const EvCalculator = () => {
    const [eventQuery, setEventQuery] = useState('');
    const [odds, setOdds] = useState('');
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleCalculate = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setResult(null);

        try {
            const res = await calculateExpectedValue(eventQuery, parseFloat(odds));
            setResult(res.data);
        } catch (err) {
            setError('Failed to fetch predictive market data.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-3xl mx-auto space-y-10">
            <header className="pb-8 border-b border-hairline">
                <h2 className="display-md">+EV Engine</h2>
                <p className="text-body mt-2">Compare bookmaker odds against Kalshi/Polymarket true probabilities.</p>
            </header>

            <div className="bg-surface-card p-10 rounded-lg border border-hairline">
                <form onSubmit={handleCalculate} className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <label className="block text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-3">Event Search Query</label>
                            <input
                                type="text"
                                required
                                placeholder="e.g. Real Madrid"
                                className="input-field"
                                value={eventQuery}
                                onChange={(e) => setEventQuery(e.target.value)}
                            />
                        </div>
                        <div>
                            <label className="block text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-3">Bookmaker Odds (Decimal)</label>
                            <input
                                type="number"
                                required
                                step="0.01"
                                placeholder="2.15"
                                className="input-field"
                                value={odds}
                                onChange={(e) => setOdds(e.target.value)}
                            />
                        </div>
                    </div>
                    
                    <button type="submit" disabled={loading} className="button-primary !h-12 w-full uppercase tracking-widest font-black text-xs disabled:opacity-50">
                        {loading ? 'Processing...' : 'Calculate Expected Value'}
                    </button>
                    {error && <p className="text-rose-500 text-sm mt-2 font-bold">{error}</p>}
                </form>
            </div>

            {result && (
                <div className={`p-10 rounded-lg border flex flex-col items-center text-center ${result.positiveEv ? 'bg-primary/5 border-primary/50' : 'bg-rose-500/5 border-rose-500/50'}`}>
                    <h4 className="text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-8">Arbitrage Analysis</h4>
                    
                    <div className="grid grid-cols-3 gap-8 w-full mb-8 border-b border-hairline pb-8">
                        <div>
                            <p className="text-xs font-semibold text-muted mb-2">Bookmaker Odds</p>
                            <p className="text-2xl font-bold text-on-dark font-numeric">{result.bookmakerOdds.toFixed(2)}</p>
                        </div>
                        <div>
                            <p className="text-xs font-semibold text-muted mb-2">Market Probability</p>
                            <p className="text-2xl font-bold text-on-dark font-numeric">{(result.trueProbability * 100).toFixed(1)}%</p>
                        </div>
                        <div>
                            <p className="text-xs font-semibold text-muted mb-2">Fair Odds</p>
                            <p className="text-2xl font-bold text-on-dark font-numeric">{(1 / result.trueProbability).toFixed(2)}</p>
                        </div>
                    </div>

                    <div className="flex flex-col items-center">
                        <p className="text-[10px] font-black text-muted uppercase tracking-[0.2em] mb-2">Expected Value (+EV)</p>
                        <p className={`text-6xl font-black font-numeric ${result.positiveEv ? 'text-primary' : 'text-rose-500'}`}>
                            {result.expectedValuePercentage > 0 ? '+' : ''}{result.expectedValuePercentage.toFixed(2)}%
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EvCalculator;
