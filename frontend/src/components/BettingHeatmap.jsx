import React, { useEffect, useState } from 'react';
import { getHeatmapData } from '../api';
import { formatSignedMoney } from '../utils/currency';

const BettingHeatmap = () => {
  const [heatmapData, setHeatmapData] = useState({});
  const [displayCurrency, setDisplayCurrency] = useState('PLN');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHeatmapData();
  }, []);

  const fetchHeatmapData = async () => {
    try {
      const response = await getHeatmapData();
      setHeatmapData(response.data?.dailyProfit || {});
      setDisplayCurrency(response.data?.displayCurrency || 'PLN');
    } catch (error) {
      console.error("Failed to fetch heatmap data:", error);
    } finally {
      setLoading(false);
    }
  };

  const [hoveredDay, setHoveredDay] = useState(null);

  // ... fetchHeatmapData (unchanged)

  const getColor = (profit) => {
    if (profit === undefined) return 'bg-hairline/50';
    if (profit > 0) {
      if (profit > 100) return 'bg-emerald-400';
      if (profit > 50) return 'bg-emerald-500';
      return 'bg-emerald-600/40';
    } else if (profit < 0) {
      if (profit < -100) return 'bg-rose-400';
      if (profit < -50) return 'bg-accent-rose';
      return 'bg-rose-600/40';
    }
    return 'bg-hairline/50';
  };

  const renderHeatmap = () => {
    const days = [];
    const today = new Date();
    for (let i = 89; i >= 0; i--) {
      const d = new Date();
      d.setDate(today.getDate() - i);
      const dateStr = d.toISOString().split('T')[0];
      const profit = heatmapData[dateStr];
      
      days.push(
        <div 
          key={dateStr} 
          onMouseEnter={() => setHoveredDay({ date: dateStr, profit })}
          onMouseLeave={() => setHoveredDay(null)}
          className={`w-3.5 h-3.5 rounded-[2px] ${getColor(profit)} cursor-crosshair transition-all duration-200 hover:scale-125 hover:z-10 relative`}
        >
          {hoveredDay?.date === dateStr && (
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-40 bg-surface-elevated border border-hairline text-[10px] p-3 rounded-lg z-50 pointer-events-none">
              <div className="font-black border-b border-hairline pb-2 mb-2 uppercase tracking-widest text-muted">{dateStr}</div>
              <div className={`text-xs font-bold ${profit > 0 ? 'text-emerald-400' : profit < 0 ? 'text-rose-400' : 'text-on-dark'}`}>
                {profit !== undefined ? formatSignedMoney(profit, displayCurrency) : 'IDLE SESSION'}
              </div>
              <div className="absolute top-full left-1/2 -translate-x-1/2 border-8 border-transparent border-t-surface-elevated"></div>
            </div>
          )}
        </div>
      );
    }
    return days;
  };

  if (loading) return <div className="text-muted font-bold text-xs uppercase tracking-widest animate-pulse">Initializing Grid...</div>;

  return (
    <div>
      <div className="flex flex-wrap gap-1.5 max-w-full">
        {renderHeatmap()}
      </div>
      <div className="mt-8 flex items-center gap-6 text-[10px] font-bold text-muted uppercase tracking-widest">
        <div className="flex items-center gap-2"><div className="w-2.5 h-2.5 bg-accent-rose rounded-[2px]"></div> Negative Yield</div>
        <div className="flex items-center gap-2"><div className="w-2.5 h-2.5 bg-hairline rounded-[2px]"></div> No Session</div>
        <div className="flex items-center gap-2"><div className="w-2.5 h-2.5 bg-emerald-500 rounded-[2px]"></div> Positive Yield</div>
      </div>
    </div>
  );
};

export default BettingHeatmap;
