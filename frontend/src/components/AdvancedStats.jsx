import React, { useEffect, useState } from 'react';
import { getAdvancedStats } from '../api';

const StatCard = ({ title, value, subtext, color = "text-primary" }) => (
  <div className="bg-surface-card p-8 rounded-lg border border-hairline flex flex-col items-center text-center transition-all duration-300">
    <h4 className="text-muted text-[10px] font-black uppercase tracking-[0.2em] mb-4">{title}</h4>
    <div className={`stat-display ${color}`}>{value}</div>
    {subtext && <p className="text-[10px] font-bold text-muted mt-3 uppercase tracking-widest">{subtext}</p>}
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

  if (loading) return (
      <div className="flex flex-col items-center justify-center py-10 gap-4">
          <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
          <p className="text-[10px] font-bold text-muted uppercase tracking-widest">Aggregating Risk Metrics...</p>
      </div>
  );
  if (!stats) return null;

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
      <StatCard 
        title="Sharpe Ratio" 
        value={stats.sharpeRatio ? stats.sharpeRatio.toFixed(2) : '0.00'}
        subtext="Risk-adjusted return"
      />
      <StatCard 
        title="Efficiency" 
        value={`${stats.winRatesByType?.SINGLE ? stats.winRatesByType.SINGLE.toFixed(1) : '0.0'}%`}
        subtext="Win Rate (Single)"
      />
      <div className="bg-surface-card p-8 rounded-lg border border-hairline flex flex-col items-center text-center justify-center">
        <h4 className="text-muted text-[10px] font-black uppercase tracking-[0.2em] mb-4">Streak Analysis</h4>
        <div className="font-mono text-xs font-bold text-primary flex flex-col gap-1.5 uppercase tracking-wider">
          {stats.currentStreak ? (
            stats.currentStreak.split('|').map((part, index) => (
              <span key={index} className="whitespace-nowrap bg-surface-elevated px-2 py-0.5 rounded border border-hairline">
                {part.trim()}
              </span>
            ))
          ) : (
            <span className="text-muted">No Data</span>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdvancedStats;
