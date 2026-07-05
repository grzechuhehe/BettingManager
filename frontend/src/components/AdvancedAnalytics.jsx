import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, ReferenceLine, Cell } from 'recharts';

const AdvancedAnalytics = ({ stats }) => {
    if (!stats) return null;

    const { equityCurve, profitBySport, roi, yield: yieldVal } = stats;

    const sportData = profitBySport 
        ? Object.entries(profitBySport).map(([sport, profit]) => ({ name: sport, profit }))
        : [];

    const currencyFormatter = (value) => `$${value.toFixed(2)}`;

    return (
        <div className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-surface-card p-8 rounded-lg border border-hairline flex flex-col items-center justify-center">
                    <h3 className="text-muted uppercase text-xs font-bold tracking-widest mb-4">Return on Investment (ROI)</h3>
                    <p className={`text-4xl font-bold font-numeric ${roi >= 0 ? 'text-primary' : 'text-accent-rose'}`}>
                        {roi ? `${roi.toFixed(2)}%` : '0.00%'}
                    </p>
                    <p className="text-xs text-muted mt-3 font-medium">Net Profit / Total Investment</p>
                </div>
                <div className="bg-surface-card p-8 rounded-lg border border-hairline flex flex-col items-center justify-center">
                    <h3 className="text-muted uppercase text-xs font-bold tracking-widest mb-4">Yield</h3>
                    <p className={`text-4xl font-bold font-numeric ${yieldVal >= 0 ? 'text-primary' : 'text-accent-rose'}`}>
                        {yieldVal ? `${yieldVal.toFixed(2)}%` : '0.00%'}
                    </p>
                    <p className="text-xs text-muted mt-3 font-medium">Net Profit / Total Turnover</p>
                </div>
            </div>

            {/* Equity Curve Chart */}
            <div className="bg-surface-card p-8 rounded-lg border border-hairline h-96">
                <div className="flex justify-between items-center mb-8">
                    <h3 className="text-on-dark font-bold text-lg">📉 Equity Curve</h3>
                    <span className="text-[10px] font-bold px-2 py-1 bg-surface-elevated text-body-strong rounded-full uppercase tracking-widest">Lifetime Performance</span>
                </div>
                <ResponsiveContainer width="100%" height="80%">
                    <LineChart data={equityCurve} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#2a2a2a" vertical={false} />
                        <XAxis 
                            dataKey="date" 
                            tick={{fontSize: 11, fill: '#888888'}} 
                            tickMargin={10}
                            axisLine={{ stroke: '#2a2a2a' }}
                        />
                        <YAxis 
                            tick={{fontSize: 11, fill: '#888888'}} 
                            tickFormatter={(value) => `$${value}`}
                            axisLine={{ stroke: '#2a2a2a' }}
                        />
                        <Tooltip 
                            contentStyle={{ backgroundColor: '#242424', border: '1px solid #3a3a3a', borderRadius: '8px', color: '#ffffff' }}
                            itemStyle={{ color: '#faff69' }}
                            formatter={(value) => [currencyFormatter(value), "Cumulative Profit"]}
                        />
                        <ReferenceLine y={0} stroke="#3a3a3a" strokeDasharray="3 3" />
                        <Line 
                            type="monotone" 
                            dataKey="cumulativeProfit" 
                            stroke="#faff69" 
                            strokeWidth={3} 
                            dot={{ r: 0 }} 
                            activeDot={{ r: 6, fill: '#faff69', stroke: '#0a0a0a', strokeWidth: 2 }} 
                        />
                    </LineChart>
                </ResponsiveContainer>
            </div>

            {/* Profit by Sport Chart */}
            <div className="bg-surface-card p-8 rounded-lg border border-hairline h-80">
                <h3 className="text-on-dark font-bold text-lg mb-6">🏆 Profit by Sport</h3>
                <ResponsiveContainer width="100%" height="80%">
                    <BarChart data={sportData} layout="vertical" margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#2a2a2a" />
                        <XAxis type="number" tick={{fontSize: 11, fill: '#888888'}} axisLine={{ stroke: '#2a2a2a' }} tickFormatter={(value) => `$${value}`} />
                        <YAxis dataKey="name" type="category" tick={{fontSize: 11, fill: '#e6e6e6', fontWeight: 500}} width={100} axisLine={{ stroke: '#2a2a2a' }} />
                        <Tooltip 
                            cursor={{fill: '#121212'}} 
                            formatter={(value) => currencyFormatter(value)}
                            contentStyle={{ backgroundColor: '#1a1a1a', border: '1px solid #2a2a2a', borderRadius: '8px' }}
                            itemStyle={{ color: '#faff69', fontWeight: 'bold', fontSize: '12px' }}
                            labelStyle={{ color: '#ffffff', marginBottom: '4px', fontWeight: 'bold', fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.1em' }}
                        />
                        <Bar dataKey="profit" radius={[0, 4, 4, 0]} barSize={20}>
                            {sportData.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={entry.profit >= 0 ? '#22c55e' : '#ef4444'} />
                            ))}
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export default AdvancedAnalytics;
