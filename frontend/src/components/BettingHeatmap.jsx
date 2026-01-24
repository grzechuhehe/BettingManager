import React, { useEffect, useState } from 'react';
import { getHeatmapData } from '../api';

const BettingHeatmap = () => {
  const [heatmapData, setHeatmapData] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHeatmapData();
  }, []);

  const fetchHeatmapData = async () => {
    try {
      const response = await getHeatmapData();
      setHeatmapData(response.data); // Oczekujemy formatu: { "2023-10-01": 150.00, ... }
    } catch (error) {
      console.error("Failed to fetch heatmap data:", error);
    } finally {
      setLoading(false);
    }
  };

  const [hoveredDay, setHoveredDay] = useState(null);

  // ... fetchHeatmapData (unchanged)

  const getColor = (profit) => {
    if (!profit) return 'bg-gray-200'; // Pusty dzień
    if (profit > 0) {
      if (profit > 100) return 'bg-green-500';
      if (profit > 50) return 'bg-green-400';
      return 'bg-green-300';
    } else {
      if (profit < -100) return 'bg-red-500';
      if (profit < -50) return 'bg-red-400';
      return 'bg-red-300';
    }
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
          className={`w-4 h-4 rounded-sm ${getColor(profit)} cursor-pointer transition-all duration-200 hover:scale-125 relative`}
        >
          {hoveredDay?.date === dateStr && (
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-32 bg-gray-900 text-white text-[10px] p-2 rounded shadow-xl z-50 pointer-events-none">
              <div className="font-bold border-b border-gray-700 pb-1 mb-1">{dateStr}</div>
              <div className={profit > 0 ? 'text-green-400' : profit < 0 ? 'text-red-400' : 'text-gray-400'}>
                {profit !== undefined ? `${profit >= 0 ? '+' : '-'}$${Math.abs(profit).toFixed(2)}` : 'No activity'}
              </div>
              <div className="absolute top-full left-1/2 -translate-x-1/2 border-8 border-transparent border-t-gray-900"></div>
            </div>
          )}
        </div>
      );
    }
    return days;
  };

  if (loading) return <div className="text-gray-400">Loading Heatmap...</div>;

  return (
    <div className="bg-white p-6 rounded-lg shadow-md border border-gray-100">
      <h3 className="text-lg font-semibold text-gray-800 mb-4">Activity Heatmap (Last 90 Days)</h3>
      <div className="flex flex-wrap gap-1 max-w-full">
        {renderHeatmap()}
      </div>
      <div className="mt-4 flex items-center gap-4 text-xs text-gray-500">
        <div className="flex items-center gap-1"><div className="w-3 h-3 bg-red-500 rounded-sm"></div> Loss</div>
        <div className="flex items-center gap-1"><div className="w-3 h-3 bg-gray-200 rounded-sm"></div> No Bet</div>
        <div className="flex items-center gap-1"><div className="w-3 h-3 bg-green-500 rounded-sm"></div> Profit</div>
      </div>
    </div>
  );
};

export default BettingHeatmap;
