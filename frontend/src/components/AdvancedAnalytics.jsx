import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, Legend, ReferenceLine } from 'recharts';

const AdvancedAnalytics = ({ stats }) => {
    if (!stats) return null;

    const { equityCurve, profitBySport, roi, yield: yieldVal } = stats;

    // Przekształć profitBySport (Map) na tablicę dla Recharts
    const sportData = profitBySport 
        ? Object.entries(profitBySport).map(([sport, profit]) => ({ name: sport, profit }))
        : [];

    // Formatowanie waluty dla tooltipów
    const currencyFormatter = (value) => `$${value.toFixed(2)}`;

    return (
        <div className="space-y-6 animate-fade-in-up">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* ROI & Yield Cards - Bloomberg Style */}
                <div className="bg-gradient-to-br from-white to-gray-50 p-6 rounded-lg shadow-sm border border-gray-200 flex flex-col items-center justify-center">
                    <h3 className="text-gray-500 uppercase text-xs font-bold tracking-wider mb-2">Return on Investment (ROI)</h3>
                    <p className={`text-4xl font-extrabold ${roi >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {roi ? `${roi.toFixed(2)}%` : '0.00%'}
                    </p>
                    <p className="text-xs text-gray-400 mt-2">Net Profit / Total Investment</p>
                </div>
                <div className="bg-gradient-to-br from-white to-gray-50 p-6 rounded-lg shadow-sm border border-gray-200 flex flex-col items-center justify-center">
                    <h3 className="text-gray-500 uppercase text-xs font-bold tracking-wider mb-2">Yield</h3>
                    <p className={`text-4xl font-extrabold ${yieldVal >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {yieldVal ? `${yieldVal.toFixed(2)}%` : '0.00%'}
                    </p>
                    <p className="text-xs text-gray-400 mt-2">Net Profit / Total Turnover</p>
                </div>
            </div>

            {/* Equity Curve Chart */}
            <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 h-96">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-gray-800 font-bold text-lg">📈 Equity Curve</h3>
                    <span className="text-xs font-medium px-2 py-1 bg-blue-100 text-blue-700 rounded-full">Lifetime Performance</span>
                </div>
                <ResponsiveContainer width="100%" height="90%">
                    <LineChart data={equityCurve} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                        <XAxis 
                            dataKey="date" 
                            tick={{fontSize: 12, fill: '#6b7280'}} 
                            tickMargin={10}
                        />
                        <YAxis 
                            tick={{fontSize: 12, fill: '#6b7280'}} 
                            tickFormatter={(value) => `$${value}`}
                        />
                        <Tooltip 
                            contentStyle={{ backgroundColor: '#1f2937', color: '#fff', border: 'none', borderRadius: '8px' }}
                            itemStyle={{ color: '#fff' }}
                            formatter={(value) => [currencyFormatter(value), "Profit"]}
                            labelFormatter={(label) => `Date: ${label}`}
                        />
                        <ReferenceLine y={0} stroke="#9ca3af" strokeDasharray="3 3" />
                        <Line 
                            type="monotone" 
                            dataKey="cumulativeProfit" 
                            stroke="#2563eb" 
                            strokeWidth={3} 
                            dot={{ r: 3, fill: '#2563eb', strokeWidth: 0 }} 
                            activeDot={{ r: 6 }} 
                        />
                    </LineChart>
                </ResponsiveContainer>
            </div>

            {/* Profit by Sport Chart */}
            <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 h-80">
                <h3 className="text-gray-800 font-bold text-lg mb-4">🏆 Profit by Sport</h3>
                <ResponsiveContainer width="100%" height="90%">
                    <BarChart data={sportData} layout="vertical" margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f3f4f6" />
                        <XAxis type="number" tickFormatter={(value) => `$${value}`} />
                        <YAxis dataKey="name" type="category" tick={{fontSize: 12, fontWeight: 500}} width={100} />
                        <Tooltip 
                            cursor={{fill: '#f9fafb'}} 
                            formatter={(value) => currencyFormatter(value)}
                            contentStyle={{ borderRadius: '8px' }}
                        />
                        <Bar dataKey="profit" fill="#10b981" radius={[0, 4, 4, 0]} barSize={20} />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export default AdvancedAnalytics;
