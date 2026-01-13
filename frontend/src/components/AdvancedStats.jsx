import React, { useEffect, useState } from 'react';
import { getAdvancedStats } from '../api';

const StatCard = ({ title, value, subtext, color = "text-gray-900" }) => (
  <div className="bg-white p-6 rounded-lg shadow-md border border-gray-100 flex flex-col items-center text-center">
    <h4 className="text-gray-500 text-sm font-medium uppercase tracking-wider mb-2">{title}</h4>
    <div className={`text-3xl font-bold ${color}`}>{value}</div>
    {subtext && <p className="text-xs text-gray-400 mt-2">{subtext}</p>}
  </div>
);

const AdvancedStats = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await getAdvancedStats();
        setStats(response.data);
      } catch (error) {
        console.error("Failed to fetch advanced stats:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) return <div className="text-center py-4 text-gray-400">Loading Stats...</div>;
  if (!stats) return null;

  const getRoiColor = (roi) => {
    if (roi > 0) return "text-green-600";
    if (roi < 0) return "text-red-600";
    return "text-gray-600";
  };

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      <StatCard 
        title="ROI" 
        value={`${stats.roiPercentage ? stats.roiPercentage.toFixed(2) : '0.00'}%`}
        color={getRoiColor(stats.roiPercentage)}
        subtext="Return on Investment"
      />
      <StatCard 
        title="Sharpe Ratio" 
        value={stats.sharpeRatio ? stats.sharpeRatio.toFixed(2) : '0.00'}
        subtext="Risk-adjusted return"
      />
      <StatCard 
        title="Win Rate (Single)" 
        value={`${stats.winRatesByType?.SINGLE ? stats.winRatesByType.SINGLE.toFixed(1) : '0.0'}%`}
        color="text-blue-600"
      />
      <div className="bg-white p-6 rounded-lg shadow-md border border-gray-100 flex flex-col items-center text-center justify-center">
        <h4 className="text-gray-500 text-sm font-medium uppercase tracking-wider mb-2">Streak Analysis</h4>
        <div className="text-base font-bold text-yellow-600 flex flex-col gap-1">
          {stats.currentStreak ? (
            stats.currentStreak.split('|').map((part, index) => (
              <span key={index} className="whitespace-nowrap">
                {part.trim()}
              </span>
            ))
          ) : (
            "None"
          )}
        </div>
      </div>
    </div>
  );
};

export default AdvancedStats;
