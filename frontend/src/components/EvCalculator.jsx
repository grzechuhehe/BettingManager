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

            <div className="surface-card">
                <form onSubmit={handleCalculate} className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <label className="field-label">Event search query</label>
                            <input type="text" required placeholder="e.g. Real Madrid" className="input-field" value={eventQuery} onChange={(e) => setEventQuery(e.target.value)} />
                        </div>
                        <div>
                            <label className="field-label">Bookmaker odds (decimal)</label>
                            <input type="number" required step="0.01" placeholder="2.15" className="input-field font-numeric" value={odds} onChange={(e) => setOdds(e.target.value)} />
                        </div>
                    </div>
                    <button type="submit" disabled={loading} className="button-primary w-full">{loading ? 'Processing...' : 'Calculate expected value'}</button>
                    {error && <p className="text-accent-rose body-sm mt-2">{error}</p>}
                </form>
            </div>

            {result && (
                <div className={`surface-card ${result.positiveEv ? 'border-primary' : 'border-accent-rose'}`}>
                    <h4 className="caption-uppercase text-muted mb-8 text-center">Arbitrage analysis</h4>
                    <div className="grid grid-cols-3 gap-8 w-full mb-8 border-b border-hairline pb-8">
                        <div className="text-center">
                            <p className="caption text-muted mb-2">Bookmaker odds</p>
                            <p className="title-lg font-numeric">{result.bookmakerOdds.toFixed(2)}</p>
                        </div>
                        <div className="text-center">
                            <p className="caption text-muted mb-2">Market probability</p>
                            <p className="title-lg font-numeric">{(result.trueProbability * 100).toFixed(1)}%</p>
                        </div>
                        <div className="text-center">
                            <p className="caption text-muted mb-2">Fair odds</p>
                            <p className="title-lg font-numeric">{(1 / result.trueProbability).toFixed(2)}</p>
                        </div>
                    </div>
                    <div className="text-center">
                        <p className="caption-uppercase text-muted mb-2">Expected value (+EV)</p>
                        <p className={`stat-display ${result.positiveEv ? '' : 'text-accent-rose'}`}>
                            {result.expectedValuePercentage > 0 ? '+' : ''}{result.expectedValuePercentage.toFixed(2)}%
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EvCalculator;
