
import React from 'react';
import { Link } from 'react-router-dom';

const Home = () => {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center animate-fade-in">
      <div className="mb-6 px-3 py-1 border border-primary/30 bg-primary/10 rounded-full">
         <span className="text-[10px] font-black text-primary uppercase tracking-[0.2em]">Institutional Grade Trading Tools</span>
      </div>
      <h1 className="display-xl mb-8 max-w-4xl">
        The leading <span className="text-primary">analytics platform</span> for sports traders.
      </h1>
      <p className="text-xl text-body max-w-2xl mb-14 leading-relaxed">
        Track your performance, analyze market metrics, and optimize your edge with engineering-grade technical analysis.
      </p>
      <div className="flex flex-col sm:flex-row gap-6 w-full max-w-md justify-center">
        <Link to="/register" className="button-primary !h-14 !px-10 text-base uppercase tracking-widest font-black shadow-[0_0_40px_rgba(250,255,105,0.2)]">
            Start Trading
        </Link>
        <Link to="/login" className="button-secondary !h-14 !px-10 text-base uppercase tracking-widest font-black">
            Sign In
        </Link>
      </div>
      
      {/* Feature Strip */}
      <div className="mt-32 grid grid-cols-1 md:grid-cols-3 gap-12 w-full max-w-6xl border-t border-hairline pt-16">
        <div className="text-left">
            <p className="stat-display text-3xl mb-4">2.8k+</p>
            <h3 className="text-sm font-bold text-on-dark uppercase tracking-widest mb-2">Daily Trades</h3>
            <p className="text-sm text-muted">High-frequency performance tracking across all major global sports markets.</p>
        </div>
        <div className="text-left">
            <p className="stat-display text-3xl mb-4">99.9%</p>
            <h3 className="text-sm font-bold text-on-dark uppercase tracking-widest mb-2">Accuracy</h3>
            <p className="text-sm text-muted">Precise settlement engine ensuring your ROI and Yield metrics are institutional-grade.</p>
        </div>
        <div className="text-left">
            <p className="stat-display text-3xl mb-4">15ms</p>
            <h3 className="text-sm font-bold text-on-dark uppercase tracking-widest mb-2">Latency</h3>
            <p className="text-sm text-muted">Blazing fast real-time odds and market synchronization from premium providers.</p>
        </div>
      </div>
    </div>
  );
};

export default Home;
